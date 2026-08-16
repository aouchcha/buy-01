import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { OrderList } from './order-list';
import { Order, OrderStatus, PaymentMethod } from '../../../../core/models/order';
import { environment } from '../../../../../environments/environment';

describe('OrderList', () => {
  let fixture: ComponentFixture<OrderList>;
  let component: OrderList;
  let httpTesting: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/orders`;

  const mockOrder: Order = {
    id: 'order-1',
    userId: 'user-1',
    status: OrderStatus.PENDING,
    totalAmount: 300,
    fullName: 'John Doe',
    address: '12 rue des Fleurs',
    city: 'Casablanca',
    phoneNumber: '+212612345678',
    createdAt: 1700000000000,
    cartItems: [],
    paymentMethod: PaymentMethod.CASH_ON_DELIVERY,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderList, HttpClientTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrderList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('shows an empty state when the user has no orders', () => {
    httpTesting.expectOne(apiUrl).flush([]);
    fixture.detectChanges();

    expect(component.orders()).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain("haven't placed any orders yet");
  });

  it('lists the orders returned by the backend', () => {
    httpTesting.expectOne(apiUrl).flush([mockOrder]);
    fixture.detectChanges();

    expect(component.orders()).toEqual([mockOrder]);
    expect(fixture.nativeElement.textContent).toContain('Order #order-1');
    expect(fixture.nativeElement.textContent).toContain('PENDING');
  });
});
