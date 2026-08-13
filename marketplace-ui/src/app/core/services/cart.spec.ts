import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { CartService } from './cart';
import { ProductDto } from '../models/product';

describe('CartService', () => {
  let service: CartService;

  const mockProduct: ProductDto = {
    id: 'prod-1',
    name: 'Rooster',
    description: 'A fine rooster',
    price: 150,
    quantity: 3,
    userId: 'seller-1',
    imageUrls: ['rooster.jpg'],
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('starts empty', () => {
    expect(service.items()).toEqual([]);
    expect(service.itemCount()).toBe(0);
    expect(service.total()).toBe(0);
  });

  it('add() adds a new product with quantity 1 by default', () => {
    service.add(mockProduct);

    expect(service.items()).toEqual([
      {
        productId: 'prod-1',
        name: 'Rooster',
        price: 150,
        imageUrl: 'rooster.jpg',
        quantity: 1,
        maxQuantity: 3,
      },
    ]);
    expect(service.itemCount()).toBe(1);
    expect(service.total()).toBe(150);
  });

  it('add() increases quantity when the product is already in the cart', () => {
    service.add(mockProduct);
    service.add(mockProduct);

    expect(service.items()[0].quantity).toBe(2);
    expect(service.itemCount()).toBe(2);
    expect(service.total()).toBe(300);
  });

  it('add() caps quantity at the product stock', () => {
    service.add(mockProduct, 5);

    expect(service.items()[0].quantity).toBe(3);
  });

  it('updateQuantity() updates the quantity of an existing item', () => {
    service.add(mockProduct);
    service.updateQuantity('prod-1', 2);

    expect(service.items()[0].quantity).toBe(2);
  });

  it('updateQuantity() caps at the stored maxQuantity', () => {
    service.add(mockProduct);
    service.updateQuantity('prod-1', 99);

    expect(service.items()[0].quantity).toBe(3);
  });

  it('updateQuantity() removes the item when quantity drops to 0 or below', () => {
    service.add(mockProduct);
    service.updateQuantity('prod-1', 0);

    expect(service.items()).toEqual([]);
  });

  it('remove() removes an item by productId', () => {
    service.add(mockProduct);
    service.remove('prod-1');

    expect(service.items()).toEqual([]);
  });

  it('clear() empties the cart', () => {
    service.add(mockProduct);
    service.clear();

    expect(service.items()).toEqual([]);
    expect(service.itemCount()).toBe(0);
  });

  it('persists items to localStorage and reloads them on a fresh instance', () => {
    service.add(mockProduct);

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const reloaded = TestBed.inject(CartService);

    expect(reloaded.items()).toHaveLength(1);
    expect(reloaded.items()[0].productId).toBe('prod-1');
  });

  it('falls back to an empty cart when localStorage holds invalid JSON', () => {
    localStorage.setItem('cart_items', 'not-json');

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const freshService = TestBed.inject(CartService);

    expect(freshService.items()).toEqual([]);
  });
});
