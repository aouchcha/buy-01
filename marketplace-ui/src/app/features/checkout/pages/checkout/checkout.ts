import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

import { Navbar } from '../../../../layout/navbar/navbar';
import { CartService } from '../../../../core/services/cart';
import { OrderService } from '../../../../core/services/order';
import { ToastService } from '../../../../core/services/toast.service';
import { CreateOrderRequest, PaymentMethod } from '../../../../core/models/order';

type CheckoutStep = 'address' | 'review' | 'payment';

@Component({
  selector: 'app-checkout',
  imports: [Navbar, RouterLink, ReactiveFormsModule, DecimalPipe],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class Checkout implements OnInit {
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly PaymentMethod = PaymentMethod;

  readonly items = this.cartService.items;
  readonly subtotal = this.cartService.total;
  readonly delivery = 0;
  readonly total = computed(() => this.subtotal() + this.delivery);

  readonly step = signal<CheckoutStep>('address');
  readonly submitting = signal(false);

  readonly addressForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9 ()-]{8,20}$/)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    address: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    if (this.items().length === 0) {
      this.router.navigate(['/cart']);
    }
  }

  continueFromAddress(): void {
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }
    this.step.set('review');
  }

  backToAddress(): void {
    this.step.set('address');
  }

  continueFromReview(): void {
    this.step.set('payment');
  }

  backToReview(): void {
    this.step.set('review');
  }

  placeOrder(): void {
    if (this.submitting()) {
      return;
    }
    if (this.addressForm.invalid || this.items().length === 0) {
      this.toastService.error('Your order could not be submitted. Please check your details.');
      return;
    }

    this.submitting.set(true);

    const { fullName, phoneNumber, city, address } = this.addressForm.getRawValue();

    const request: CreateOrderRequest = {
      fullName: fullName!,
      phoneNumber: phoneNumber!,
      city: city!,
      address: address!,
      items: this.items().map((item) => ({
        productId: item.productId,
        productName: item.name,
        price: item.price,
        quantity: item.quantity,
      })),
    };

    this.orderService.create(request).subscribe({
      next: (order) => {
        this.cartService.clear();
        this.toastService.success('Order placed successfully.');
        this.router.navigate(['/orders', order.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        const message = this.mapOrderError(err);
        if (message) {
          this.toastService.error(message);
        }
      },
    });
  }

  private mapOrderError(err: HttpErrorResponse): string | null {
    switch (err.status) {
      case 400:
        return typeof err.error === 'string' ? err.error : 'Please check your information and try again.';
      case 404:
        return 'Some products are no longer available.';
      case 409:
        return 'Insufficient stock for one or more items.';
      case 401:
      case 403:
      case 0:
        // already surfaced by the global auth interceptor
        return null;
      default:
        return 'Unable to create the order. Please try again.';
    }
  }
}
