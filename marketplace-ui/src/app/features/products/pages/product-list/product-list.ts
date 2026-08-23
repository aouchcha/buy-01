import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Navbar } from '../../../../layout/navbar/navbar';
import { CATEGORY_LABELS, Category, ProductDto } from '../../../../core/models/product';
import { Product as ProductService } from '../../../../core/services/product';
import { CartService } from '../../../../core/services/cart';
import { ToastService } from '../../../../core/services/toast.service';
import { Auth } from '../../../../core/services/auth';

@Component({
  selector: 'app-product-list',
  imports: [Navbar],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(Auth);

  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly imageIndexes = signal<Record<string, number>>({});
  readonly isLogin = computed(() => this.authService.isLoggedIn());
  readonly isSeller = computed(() => this.authService.isSeller());

  ngOnInit(): void {
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

  categoryLabel(category: Category): string {
    return CATEGORY_LABELS[category];
  }

  getImageIndex(productId: string): number {
    return this.imageIndexes()[productId] ?? 0;
  }

  nextImage(product: ProductDto, event: Event): void {
    event.stopPropagation();
    const current = this.getImageIndex(product.id);
    this.imageIndexes.update(m => ({ ...m, [product.id]: (current + 1) % product.imageUrls.length }));
  }

  prevImage(product: ProductDto, event: Event): void {
    event.stopPropagation();
    const current = this.getImageIndex(product.id);
    this.imageIndexes.update(m => ({ ...m, [product.id]: (current - 1 + product.imageUrls.length) % product.imageUrls.length }));
  }

  goToProduct(id: string): void {
    this.router.navigate(['/products', id]);
  }

  addToCart(product: ProductDto, event: Event): void {
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
