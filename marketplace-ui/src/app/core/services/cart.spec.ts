import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CartService } from './cart';
import { Cart } from '../models/cart';
import { environment } from '../../../environments/environment';

describe('CartService', () => {
  let service: CartService;
  let httpTesting: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/cart`;

  const mockCart: Cart = {
    id: 'cart-1',
    userId: 'user-1',
    cartItems: [
      {
        id: 'item-1',
        sellerId: 'seller-1',
        productId: 'prod-1',
        productName: 'Rooster',
        price: 150,
        quantity: 2,
        totalPrice: 300,
      },
    ],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });

    service = TestBed.inject(CartService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('starts empty', () => {
    expect(service.items()).toEqual([]);
    expect(service.itemCount()).toBe(0);
    expect(service.total()).toBe(0);
  });

  it('load() GETs the cart and updates items/itemCount/total', () => {
    service.load().subscribe();

    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockCart);

    expect(service.items()).toEqual(mockCart.cartItems);
    expect(service.itemCount()).toBe(2);
    expect(service.total()).toBe(300);
  });

  it('addItem() POSTs to /cart/items then reloads the cart', () => {
    service.addItem('prod-1', 2).subscribe();

    const postReq = httpTesting.expectOne(`${apiUrl}/items`);
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body).toEqual({ productId: 'prod-1', quantity: 2 });
    postReq.flush(null);

    const getReq = httpTesting.expectOne(apiUrl);
    expect(getReq.request.method).toBe('GET');
    getReq.flush(mockCart);

    expect(service.items()).toEqual(mockCart.cartItems);
  });

  it('updateQuantity() PATCHes /cart/items and applies the returned cart', () => {
    service.updateQuantity('prod-1', 3).subscribe();

    const req = httpTesting.expectOne(`${apiUrl}/items`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ productId: 'prod-1', quantity: 3 });
    req.flush(mockCart);

    expect(service.items()).toEqual(mockCart.cartItems);
  });

  it('remove() DELETEs /cart/items/{productId} and applies the returned cart', () => {
    service.remove('prod-1').subscribe();

    const req = httpTesting.expectOne(`${apiUrl}/items/prod-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ ...mockCart, cartItems: [] });

    expect(service.items()).toEqual([]);
  });

  it('clear() DELETEs /cart and empties the items', () => {
    service.load().subscribe();
    httpTesting.expectOne(apiUrl).flush(mockCart);

    service.clear().subscribe();
    httpTesting.expectOne(apiUrl).flush(null);

    expect(service.items()).toEqual([]);
    expect(service.itemCount()).toBe(0);
  });

  it('reset() empties the items locally without an HTTP call', () => {
    service.load().subscribe();
    httpTesting.expectOne(apiUrl).flush(mockCart);

    service.reset();

    expect(service.items()).toEqual([]);
  });
});
