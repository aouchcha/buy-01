import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Navbar } from '../../../../layout/navbar/navbar';
import { CATEGORY_LABELS, CATEGORY_OPTIONS, Category, ProductDto } from '../../../../core/models/product';
import { Product as ProductService } from '../../../../core/services/product';
import { CartService } from '../../../../core/services/cart';
import { ToastService } from '../../../../core/services/toast.service';
import { Auth } from '../../../../core/services/auth';
import { SearchService } from '../../../../core/services/search';
import { SORT_OPTIONS, SortBy } from '../../../../core/models/search';

interface ProductCardVm {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  category: Category;
  imageUrls: string[];
}

const PAGE_SIZE = 12;

@Component({
  selector: 'app-product-list',
  imports: [Navbar, FormsModule],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly searchService = inject(SearchService);
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(Auth);

  private debounceHandle: ReturnType<typeof setTimeout> | null = null;

  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly imageIndexes = signal<Record<string, number>>({});
  readonly isLogin = computed(() => this.authService.isLoggedIn());
  readonly isSeller = computed(() => this.authService.isSeller());

  readonly categoryOptions = CATEGORY_OPTIONS;
  readonly sortOptions = SORT_OPTIONS;

  readonly keyword = signal('');
  readonly selectedCategory = signal<Category | ''>('');
  readonly minPrice = signal<number | null>(null);
  readonly maxPrice = signal<number | null>(null);
  readonly sortBy = signal<SortBy | ''>('');

  readonly searchActive = computed(() =>
    !!this.keyword().trim() || !!this.selectedCategory() || this.minPrice() != null || this.maxPrice() != null || !!this.sortBy()
  );

  readonly searchResults = signal<ProductCardVm[]>([]);
  readonly searchLoading = signal(false);
  readonly searchError = signal<string | null>(null);
  readonly page = signal(0);
  readonly hasMore = signal(true);

  readonly displayItems = computed<ProductCardVm[]>(() =>
    this.searchActive()
      ? this.searchResults()
      : this.products().map((p) => this.fromProductDto(p))
  );

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.error.set(null);
    this.productService.getAll().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load birds right now. Please try again later.');
        this.loading.set(false);
      },
    });
  }

  onFilterChange(): void {
    if (this.debounceHandle) clearTimeout(this.debounceHandle);
    this.debounceHandle = setTimeout(() => this.runSearch(true), 300);
  }

  clearFilters(): void {
    this.keyword.set('');
    this.selectedCategory.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.sortBy.set('');
    this.searchResults.set([]);
    this.searchError.set(null);
    this.page.set(0);
  }

  loadMore(): void {
    if (!this.hasMore() || this.searchLoading()) return;
    this.page.update((p) => p + 1);
    this.runSearch(false);
  }

  private runSearch(reset: boolean): void {
    if (!this.searchActive()) {
      this.searchResults.set([]);
      return;
    }

    if (reset) {
      this.page.set(0);
      this.hasMore.set(true);
    }

    this.searchLoading.set(true);
    this.searchError.set(null);

    this.searchService
      .search({
        keyword: this.keyword().trim() || undefined,
        category: this.selectedCategory() || undefined,
        minPrice: this.minPrice() ?? undefined,
        maxPrice: this.maxPrice() ?? undefined,
        sortBy: this.sortBy() || undefined,
        page: this.page(),
        size: PAGE_SIZE,
      })
      .subscribe({
        next: (docs) => {
          const items = docs.map((d) => this.fromProductDocument(d));
          this.searchResults.update((existing) => (reset ? items : [...existing, ...items]));
          this.hasMore.set(items.length === PAGE_SIZE);
          this.searchLoading.set(false);
        },
        error: () => {
          this.searchError.set('Search is unavailable right now. Please try again later.');
          this.searchLoading.set(false);
        },
      });
  }

  private fromProductDto(p: ProductDto): ProductCardVm {
    return {
      id: p.id,
      name: p.name,
      description: p.description,
      price: p.price,
      quantity: p.quantity,
      category: p.category,
      imageUrls: p.imageUrls,
    };
  }

  private fromProductDocument(d: { id: string; productName: string; description: string; price: number; quantity: number; category: string; imageUrls: string[] }): ProductCardVm {
    return {
      id: d.id,
      name: d.productName,
      description: d.description,
      price: d.price,
      quantity: d.quantity,
      category: d.category as Category,
      imageUrls: d.imageUrls,
    };
  }

  categoryLabel(category: Category): string {
    return CATEGORY_LABELS[category];
  }

  getImageIndex(productId: string): number {
    return this.imageIndexes()[productId] ?? 0;
  }

  nextImage(product: ProductCardVm, event: Event): void {
    event.stopPropagation();
    const current = this.getImageIndex(product.id);
    this.imageIndexes.update(m => ({ ...m, [product.id]: (current + 1) % product.imageUrls.length }));
  }

  prevImage(product: ProductCardVm, event: Event): void {
    event.stopPropagation();
    const current = this.getImageIndex(product.id);
    this.imageIndexes.update(m => ({ ...m, [product.id]: (current - 1 + product.imageUrls.length) % product.imageUrls.length }));
  }

  goToProduct(id: string): void {
    this.router.navigate(['/products', id]);
  }

  addToCart(product: ProductCardVm, event: Event): void {
    event.stopPropagation();
    if (product.quantity <= 0) {
      this.toastService.error('This product is out of stock.');
      return;
    }
    this.cartService.addItem(product.id).subscribe({
      next: () => this.toastService.success(`${product.name} added to cart.`),
      error: (err: HttpErrorResponse) => this.toastService.error(this.mapAddToCartError(err)),
    });
  }

  private mapAddToCartError(err: HttpErrorResponse): string {
    switch (err.status) {
      case 404:
        return 'This product is no longer available.';
      case 409:
        return 'Not enough stock available for this item.';
      default:
        return 'Could not add this item to your cart. Please try again.';
    }
  }
}
