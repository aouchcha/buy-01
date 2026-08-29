import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { Navbar } from '../../../../layout/navbar/navbar';
import { OrderService } from '../../../../core/services/order';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmService } from '../../../../core/services/confirm';
import { Order, OrderStatus, PaymentMethod } from '../../../../core/models/order';

type TimelineState = 'done' | 'current' | 'pending' | 'cancelled';

interface TimelineStep {
  label: string;
  state: TimelineState;
}

@Component({
  selector: 'app-order-details',
  imports: [Navbar, RouterLink, DecimalPipe, DatePipe],
  templateUrl: './order-details.html',
  styleUrl: './order-details.scss',
})
export class OrderDetails {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly confirmService = inject(ConfirmService);

  readonly OrderStatus = OrderStatus;
  readonly PaymentMethod = PaymentMethod;

  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly cancelling = signal(false);
  readonly deleting = signal(false);
  readonly order = signal<Order | null>(null);

  readonly canCancel = computed(() => {
    const order = this.order();
    return order?.status === OrderStatus.PENDING || order?.status === OrderStatus.CONFIRMED;
  });

  readonly canDelete = this.canCancel;

  readonly timeline = computed<TimelineStep[]>(() => {
    const order = this.order();
    if (!order) {
      return [];
    }

    if (order.status === OrderStatus.CANCELLED) {
      return [
        { label: 'PENDING', state: 'done' },
        { label: 'CANCELLED', state: 'cancelled' },
      ];
    }

    const sequence = [OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED];
    const currentIndex = sequence.indexOf(order.status);

    return sequence.map((status, index) => ({
      label: status,
      state: index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'pending',
    }));
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.loading.set(false);
      this.notFound.set(true);
      return;
    }

    this.orderService.getById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
        } else if (err.status !== 401 && err.status !== 403) {
          this.toastService.error('Unable to load this order. Please try again.');
        }
      },
    });
  }

  cancelOrder(): void {
    const order = this.order();
    if (!order) return;

    this.confirmService
      .open({
        title: 'Cancel order',
        message: 'Cancel this order? This cannot be undone.',
        confirmText: 'Cancel order',
        danger: true,
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.cancelling.set(true);
        this.orderService.cancel(order.id).subscribe({
          next: (updated) => {
            this.cancelling.set(false);
            this.order.set(updated);
            this.toastService.success('Order cancelled.');
          },
          error: (err: HttpErrorResponse) => {
            this.cancelling.set(false);
            if (err.status === 409) {
              this.toastService.error('This order can no longer be cancelled.');
            } else {
              this.toastService.error('Could not cancel this order. Please try again.');
            }
          },
        });
      });
  }

  deleteOrder(): void {
    const order = this.order();
    if (!order) return;

    this.confirmService
      .open({
        title: 'Delete order',
        message: 'Delete this order permanently? This cannot be undone.',
        confirmText: 'Delete order',
        danger: true,
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.deleting.set(true);
        this.orderService.delete(order.id).subscribe({
          next: () => {
            this.deleting.set(false);
            this.toastService.success('Order deleted.');
            this.router.navigate(['/orders']);
          },
          error: (err: HttpErrorResponse) => {
            this.deleting.set(false);
            if (err.status === 409) {
              this.toastService.error('This order can no longer be deleted.');
            } else {
              this.toastService.error('Could not delete this order. Please try again.');
            }
          },
        });
      });
  }
}
