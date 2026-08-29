import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

import { Navbar } from '../../../../layout/navbar/navbar';
import { CartService } from '../../../../core/services/cart';
import { ConfirmService } from '../../../../core/services/confirm';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-cart',
  imports: [Navbar, RouterLink, DecimalPipe],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class Cart implements OnInit {
  private readonly cartService = inject(CartService);
  private readonly confirmService = inject(ConfirmService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  readonly items = this.cartService.items;
  readonly total = this.cartService.total;
  readonly delivery = 0;
  readonly loading = signal(true);

  readonly hasOutOfStockItems = computed(() =>
    this.items().some((item) => item.OutOfStock)
  );

  ngOnInit(): void {
    this.cartService.load().subscribe({
      next: () => {
        this.loading.set(false)
        console.log('=========================>');
        console.log('Out of stock items present:', this.hasOutOfStockItems());
        console.log(this.cartService.items);
        
      },
      error: () => {
        this.loading.set(false);
        this.toastService.error('Could not load your cart. Please try again.');
      },
    });
    ;

  }

  increment(productId: string, currentQuantity: number): void {
    this.cartService.updateQuantity(productId, currentQuantity + 1).subscribe({
      error: (err: HttpErrorResponse) => this.toastService.error(this.mapCartError(err)),
    });
  }

  decrement(productId: string, currentQuantity: number): void {
    if (currentQuantity <= 1) {
      this.remove(productId);
      return;
    }
    this.cartService.updateQuantity(productId, currentQuantity - 1).subscribe({
      error: (err: HttpErrorResponse) => this.toastService.error(this.mapCartError(err)),
    });
  }

  remove(productId: string): void {
    this.cartService.remove(productId).subscribe({
      next: () => this.toastService.info('Item removed from cart.'),
      error: () => this.toastService.error('Could not remove this item. Please try again.'),
    });
  }

  clearCart(): void {
    this.confirmService
      .open({
        title: 'Clear cart',
        message: 'Remove all items from your cart? This cannot be undone.',
        confirmText: 'Clear',
        danger: true,
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.cartService.clear().subscribe({
          next: () => this.toastService.success('Cart cleared.'),
          error: () => this.toastService.error('Could not clear your cart. Please try again.'),
        });
      });
  }

  goToCheckout(): void {
    if (this.items().length === 0) {
      return;
    }
    if (this.hasOutOfStockItems()) {
      this.toastService.error('Please remove out-of-stock items before proceeding to checkout.');
      return;
    }
    this.router.navigate(['/checkout']);
  }

  private mapCartError(err: HttpErrorResponse): string {
    switch (err.status) {
      case 404:
        return 'This product is no longer available.';
      case 409:
        return 'Not enough stock available for this item.';
      default:
        return 'Could not update your cart. Please try again.';
    }
  }
}
