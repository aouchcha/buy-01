import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

import { HttpErrorResponse } from '@angular/common/http';

import { Navbar } from '../../../../layout/navbar/navbar';
import { ProductDto } from '../../../../core/models/product';
import { User } from '../../../../core/models/user';
import { Product as ProductService } from '../../../../core/services/product';
import { UserService } from '../../../../core/services/user';
import { CartService } from '../../../../core/services/cart';
import { ToastService } from '../../../../core/services/toast.service';
import { Auth } from '../../../../core/services/auth';

@Component({
  selector: 'app-product-details',
  imports: [Navbar, CommonModule, RouterLink],
  templateUrl: './product-details.html',
  styleUrl: './product-details.scss',
})
export class ProductDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly productService = inject(ProductService);
  private readonly userService = inject(UserService);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(Auth);
  readonly product = signal<ProductDto | null>(null);
  readonly seller = signal<User | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly imageIndex = signal(0);
  readonly selectedQuantity = signal(1);

   readonly isLogin = computed(() => this.authService.isLoggedIn());
  readonly isSeller = computed(() => this.authService.isSeller());

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Product not found.');
      this.loading.set(false);
      return;
    }

    this.productService.getById(id).subscribe({
      next: (p) => {
        this.product.set(p);
        this.loading.set(false);

        this.userService.getPublicUser(p.userId).subscribe({
          next: (u) => this.seller.set(u),
          error: () => {},
        });
      },
      error: () => {
        this.error.set('Product not found or unavailable.');
        this.loading.set(false);
      },
    });
  }

  get currentImage(): string {
    const p = this.product();
    if (!p?.imageUrls.length) return 'assets/images/background.png';
    return p.imageUrls[this.imageIndex()];
  }

  nextImage(): void {
    const p = this.product();
    if (!p?.imageUrls.length) return;
    this.imageIndex.update(i => (i + 1) % p.imageUrls.length);
  }

  prevImage(): void {
    const p = this.product();
    if (!p?.imageUrls.length) return;
    this.imageIndex.update(i => (i - 1 + p.imageUrls.length) % p.imageUrls.length);
  }

  sellerInitials(): string {
    const s = this.seller();
    if (!s) return '?';
    return `${s.firstName[0]}${s.lastName[0]}`.toUpperCase();
  }

  incrementQuantity(): void {
    const p = this.product();
    if (!p) return;
    this.selectedQuantity.update((q) => Math.min(q + 1, p.quantity));
  }

  decrementQuantity(): void {
    this.selectedQuantity.update((q) => Math.max(q - 1, 1));
  }

  addToCart(): void {
    const p = this.product();
    if (!p || p.quantity <= 0) return;
    this.cartService.addItem(p.id, this.selectedQuantity()).subscribe({
      next: () => {
        this.toastService.success(`${p.name} added to cart.`);
        this.selectedQuantity.set(1);
      },
      error: (err: HttpErrorResponse) => {
        this.toastService.error(this.mapAddToCartError(err));
      },
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
