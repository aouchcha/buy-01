import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';

import { Cart, CartItem } from '../models/cart';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/cart`;

  private readonly itemsSignal = signal<CartItem[]>([]);

  readonly items = this.itemsSignal.asReadonly();
  readonly itemCount = computed(() => this.itemsSignal().reduce((sum, item) => sum + item.quantity, 0));
  readonly total = computed(() => this.itemsSignal().reduce((sum, item) => sum + item.totalPrice, 0));

  load(): Observable<Cart> {
    return this.http.get<Cart>(this.apiUrl).pipe(tap((cart) => this.itemsSignal.set(cart.cartItems)));
  }

  addItem(productId: string, quantity = 1): Observable<Cart> {
    return this.http
      .post<void>(`${this.apiUrl}/items`, { productId, quantity })
      .pipe(switchMap(() => this.load()));
  }

  updateQuantity(productId: string, quantity: number): Observable<Cart> {
    return this.http
      .patch<Cart>(`${this.apiUrl}/items`, { productId, quantity })
      .pipe(tap((cart) => this.itemsSignal.set(cart.cartItems)));
  }

  remove(productId: string): Observable<Cart> {
    return this.http
      .delete<Cart>(`${this.apiUrl}/items/${productId}`)
      .pipe(tap((cart) => this.itemsSignal.set(cart.cartItems)));
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.apiUrl).pipe(tap(() => this.itemsSignal.set([])));
  }

  reset(): void {
    this.itemsSignal.set([]);
  }
}
