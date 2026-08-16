import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
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
export class Cart {
  private readonly cartService = inject(CartService);
  private readonly confirmService = inject(ConfirmService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  readonly items = this.cartService.items;
  readonly total = this.cartService.total;
  readonly delivery = 0;

  increment(productId: string, currentQuantity: number, maxQuantity: number): void {
    if (currentQuantity >= maxQuantity) {
      this.toastService.info('No more stock available for this item.');
      return;
    }
    this.cartService.updateQuantity(productId, currentQuantity + 1);
  }

  decrement(productId: string, currentQuantity: number): void {
    this.cartService.updateQuantity(productId, currentQuantity - 1);
  }

  remove(productId: string): void {
    this.cartService.remove(productId);
    this.toastService.info('Item removed from cart.');
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
        if (confirmed) {
          this.cartService.clear();
          this.toastService.success('Cart cleared.');
        }
      });
  }

  goToCheckout(): void {
    if (this.items().length === 0) {
      return;
    }
    this.router.navigate(['/checkout']);
  }
}
