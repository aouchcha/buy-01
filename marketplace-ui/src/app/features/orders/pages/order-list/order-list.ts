import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { Navbar } from '../../../../layout/navbar/navbar';
import { OrderService } from '../../../../core/services/order';
import { ToastService } from '../../../../core/services/toast.service';
import { Order } from '../../../../core/models/order';

@Component({
  selector: 'app-order-list',
  imports: [Navbar, RouterLink, DecimalPipe, DatePipe],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderList {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);

  readonly loading = signal(true);
  readonly orders = signal<Order[]>([]);

  constructor() {
    this.orderService.getAll().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status !== 401 && err.status !== 403) {
          this.toastService.error('Unable to load your orders. Please try again.');
        }
      },
    });
  }
}
