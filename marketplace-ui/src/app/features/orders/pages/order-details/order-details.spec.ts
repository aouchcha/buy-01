import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { OrderDetails } from './order-details';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmService } from '../../../../core/services/confirm';
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
    postalCode: '60000',
    phoneNumber: '+212612345678',
    createdAt: 1700000000000,
    cartItems: [
      { productId: 'prod-1', productName: 'Rooster', sellerId: 'seller-1', price: 150, quantity: 2, totalPrice: 300 },
    ],
    paymentMethod: PaymentMethod.CASH_ON_DELIVERY,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderDetails, MatDialogModule, HttpClientTestingModule],
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

  it('shows the Cancel order button for a PENDING order', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    fixture.detectChanges();

    const button: HTMLButtonElement | null = fixture.nativeElement.querySelector('.btn-danger');
    expect(button).not.toBeNull();
    expect(button?.textContent).toContain('Cancel order');
  });

  it('hides the Cancel and Delete order buttons once the order is delivered', () => {
    httpTesting.expectOne(apiUrl).flush({ ...baseOrder, status: OrderStatus.DELIVERED });
    fixture.detectChanges();

    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.btn-danger'));
    expect(buttons).toHaveLength(0);
  });

  it('shows the Delete order button for a PENDING order', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    fixture.detectChanges();

    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.btn-danger'));
    expect(buttons.some((b) => b.textContent?.includes('Delete order'))).toBe(true);
  });

  it('cancelOrder() does nothing when the user does not confirm', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    const confirmService = TestBed.inject(ConfirmService);
    vi.spyOn(confirmService, 'open').mockReturnValue(of(false));

    component.cancelOrder();

    httpTesting.verify();
  });

  it('cancelOrder() PATCHes the cancel endpoint and updates the order on confirm', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    const confirmService = TestBed.inject(ConfirmService);
    const toast = TestBed.inject(ToastService);
    vi.spyOn(confirmService, 'open').mockReturnValue(of(true));
    vi.spyOn(toast, 'success');

    component.cancelOrder();

    const req = httpTesting.expectOne(`${apiUrl}/cancel`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ ...baseOrder, status: OrderStatus.CANCELLED });

    expect(component.order()?.status).toBe(OrderStatus.CANCELLED);
    expect(component.canCancel()).toBe(false);
    expect(toast.success).toHaveBeenCalledWith('Order cancelled.');
  });

  it('shows an error toast when the order can no longer be cancelled', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    const confirmService = TestBed.inject(ConfirmService);
    const toast = TestBed.inject(ToastService);
    vi.spyOn(confirmService, 'open').mockReturnValue(of(true));
    vi.spyOn(toast, 'error');

    component.cancelOrder();

    httpTesting
      .expectOne(`${apiUrl}/cancel`)
      .flush('Only pending or confirmed orders can be cancelled', { status: 409, statusText: 'Conflict' });

    expect(toast.error).toHaveBeenCalledWith('This order can no longer be cancelled.');
    expect(component.cancelling()).toBe(false);
  });

  it('deleteOrder() does nothing when the user does not confirm', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    const confirmService = TestBed.inject(ConfirmService);
    vi.spyOn(confirmService, 'open').mockReturnValue(of(false));

    component.deleteOrder();

    httpTesting.verify();
  });

  it('deleteOrder() DELETEs the order and navigates back to the order list on confirm', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    const confirmService = TestBed.inject(ConfirmService);
    const toast = TestBed.inject(ToastService);
    const router = TestBed.inject(Router);
    vi.spyOn(confirmService, 'open').mockReturnValue(of(true));
    vi.spyOn(toast, 'success');
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.deleteOrder();

    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(toast.success).toHaveBeenCalledWith('Order deleted.');
    expect(router.navigate).toHaveBeenCalledWith(['/orders']);
  });

  it('shows an error toast when the order can no longer be deleted', () => {
    httpTesting.expectOne(apiUrl).flush(baseOrder);
    const confirmService = TestBed.inject(ConfirmService);
    const toast = TestBed.inject(ToastService);
    vi.spyOn(confirmService, 'open').mockReturnValue(of(true));
    vi.spyOn(toast, 'error');

    component.deleteOrder();

    httpTesting
      .expectOne(apiUrl)
      .flush('Only pending or confirmed orders can be deleted', { status: 409, statusText: 'Conflict' });

    expect(toast.error).toHaveBeenCalledWith('This order can no longer be deleted.');
    expect(component.deleting()).toBe(false);
  });
});
