import { Injectable, computed, signal } from '@angular/core';

import { CartItem } from '../models/cart';
import { ProductDto } from '../models/product';

const STORAGE_KEY = 'cart_items';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly itemsSignal = signal<CartItem[]>(this.readFromStorage());

  readonly items = this.itemsSignal.asReadonly();
  readonly itemCount = computed(() => this.itemsSignal().reduce((sum, item) => sum + item.quantity, 0));
  readonly total = computed(() =>
    this.itemsSignal().reduce((sum, item) => sum + item.price * item.quantity, 0),
  );

  add(product: ProductDto, quantity = 1): void {
    const existing = this.itemsSignal().find((item) => item.productId === product.id);
    const maxQuantity = product.quantity;

    if (existing) {
      this.setQuantity(product.id, existing.quantity + quantity, maxQuantity);
      return;
    }

    const newItem: CartItem = {
      productId: product.id,
      name: product.name,
      price: product.price,
      imageUrl: product.imageUrls[0] ?? null,
      quantity: Math.min(Math.max(quantity, 1), Math.max(maxQuantity, 0)),
      maxQuantity,
    };

    this.itemsSignal.update((items) => [...items, newItem]);
    this.persist();
  }

  updateQuantity(productId: string, quantity: number): void {
    const existing = this.itemsSignal().find((item) => item.productId === productId);
    if (!existing) return;
    this.setQuantity(productId, quantity, existing.maxQuantity);
  }

  remove(productId: string): void {
    this.itemsSignal.update((items) => items.filter((item) => item.productId !== productId));
    this.persist();
  }

  clear(): void {
    this.itemsSignal.set([]);
    this.persist();
  }

  private setQuantity(productId: string, quantity: number, maxQuantity: number): void {
    const capped = Math.min(quantity, Math.max(maxQuantity, 0));

    if (capped <= 0) {
      this.remove(productId);
      return;
    }

    this.itemsSignal.update((items) =>
      items.map((item) => (item.productId === productId ? { ...item, quantity: capped } : item)),
    );
    this.persist();
  }

  private persist(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.itemsSignal()));
  }

  private readFromStorage(): CartItem[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as CartItem[]) : [];
    } catch {
      return [];
    }
  }
}
