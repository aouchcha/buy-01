import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Navbar } from '../../../../layout/navbar/navbar';
import { ProductDto } from '../../../../core/models/product';
import { Product as ProductService } from '../../../../core/services/product';

@Component({
  selector: 'app-product-list',
  imports: [Navbar],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);

  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly imageIndexes = signal<Record<string, number>>({});

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
}
