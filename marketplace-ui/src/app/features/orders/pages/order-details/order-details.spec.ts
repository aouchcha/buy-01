import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { OrderDetails } from './order-details';
import { ToastService } from '../../../../core/services/toast.service';
import { Order, OrderStatus, PaymentMethod } from '../../../../core/models/order';
import { environment } from '../../../../../environments/environment';

describe('OrderDetails', () => {
  let fixture: ComponentFixture<OrderDetails>;
  let component: OrderDetails;
  let httpTesting: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/orders/order-1`;

  const baseOrder: Order = {
    id: 'order-1',
    userId: 'user-1',
    status: OrderStatus.PENDING,
    totalAmount: 300,
    fullName: 'John Doe',
    address: '12 rue des Fleurs',
    city: 'Casablanca',
    phoneNumber: '+212612345678',
    createdAt: 1700000000000,
    cartItems: [
      { productId: 'prod-1', productName: 'Rooster', sellerId: 'seller-1', price: 150, quantity: 2, totalPrice: 300 },
    ],
    paymentMethod: PaymentMethod.CASH_ON_DELIVERY,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderDetails, HttpClientTestingModule],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'order-1' }) } },
        },
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrderDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
    vi.clearAllMocks();
  });

  it('shows a loading state before the order resolves', () => {
    expect(component.loading()).toBe(true);

    httpTesting.expectOne(apiUrl).flush(baseOrder);
  });

  it('displays the order once it is loaded', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.order()).toEqual(baseOrder);
    expect(fixture.nativeElement.textContent).toContain('Order #order-1');
    expect(fixture.nativeElement.textContent).toContain('Rooster');
    expect(fixture.nativeElement.textContent).toContain('PENDING');
  });

  it('builds a PENDING timeline with the current step highlighted', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);

    expect(component.timeline()).toEqual([
      { label: 'PENDING', state: 'current' },
      { label: 'CONFIRMED', state: 'pending' },
      { label: 'SHIPPED', state: 'pending' },
      { label: 'DELIVERED', state: 'pending' },
    ]);
  });

  it('builds a two-step timeline when the order is cancelled', () => {
    httpTesting.expectOne(apiUrl).flush({ ...baseOrder, status: OrderStatus.CANCELLED });

    expect(component.timeline()).toEqual([
      { label: 'PENDING', state: 'done' },
      { label: 'CANCELLED', state: 'cancelled' },
    ]);
  });

  it('shows a not-found state when the order does not exist or is not owned by the user', () => {
    httpTesting.expectOne(apiUrl).flush('Order not found', { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(component.notFound()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain("couldn't find this order");
  });

  it('shows a friendly error toast on server errors without exposing raw details', () => {
    const toast = TestBed.inject(ToastService);
    vi.spyOn(toast, 'error');

    httpTesting.expectOne(apiUrl).flush('Internal error', { status: 500, statusText: 'Server Error' });

    expect(toast.error).toHaveBeenCalledWith('Unable to load this order. Please try again.');
  });
});
