import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { OrderService } from './order';
import { CreateOrderRequest, Order, OrderStatus, PaymentMethod } from '../models/order';
import { environment } from '../../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let httpTesting: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/orders`;

  const mockRequest: CreateOrderRequest = {
    shippingAddress: {
      fullName: 'John Doe',
      phone: '+212612345678',
      city: 'Casablanca',
      postalCode: '60000',
      address: '12 rue des Fleurs',
    },
    paymentMethod: PaymentMethod.CASH_ON_DELIVERY,
  };

  const mockOrder: Order = {
    id: 'order-1',
    userId: 'user-1',
    status: OrderStatus.PENDING,
    totalAmount: 300,
    fullName: 'John Doe',
    address: '12 rue des Fleurs',
    city: 'Casablanca',
    postalCode: '60000',
    phoneNumber: '+212612345678',
    createdAt: 1700000000000,
    cartItems: [
      { productId: 'prod-1', productName: 'Rooster', sellerId: 'seller-1', price: 150, quantity: 2, totalPrice: 300 },
    ],
    paymentMethod: PaymentMethod.CASH_ON_DELIVERY,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });

    service = TestBed.inject(OrderService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('create() should POST to /orders with the request body', () => {
    let result: Order | undefined;

    service.create(mockRequest).subscribe((data) => (result = data));

    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRequest);
    req.flush(mockOrder);

    expect(result).toEqual(mockOrder);
  });

  it('getById() should GET /orders/{id}', () => {
    let result: Order | undefined;

    service.getById('order-1').subscribe((data) => (result = data));

    const req = httpTesting.expectOne(`${apiUrl}/order-1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockOrder);

    expect(result).toEqual(mockOrder);
  });

  it('getAll() should GET /orders', () => {
    let result: Order[] | undefined;

    service.getAll().subscribe((data) => (result = data));

    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush([mockOrder]);

    expect(result).toEqual([mockOrder]);
  });
});
