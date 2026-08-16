import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { Checkout } from './checkout';
import { CartService } from '../../../../core/services/cart';
import { OrderService } from '../../../../core/services/order';
import { ProductDto } from '../../../../core/models/product';
import { Order, OrderStatus, PaymentMethod } from '../../../../core/models/order';
import { environment } from '../../../../../environments/environment';

describe('Checkout', () => {
  let component: Checkout;
  let fixture: ComponentFixture<Checkout>;
  let cartService: CartService;
  let router: Router;
  let httpTesting: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/orders`;

  const mockProduct: ProductDto = {
    id: 'prod-1',
    name: 'Rooster',
    description: 'A fine rooster',
    price: 150,
    quantity: 5,
    userId: 'seller-1',
    imageUrls: ['rooster.jpg'],
  };

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
    cartItems: [
      { productId: 'prod-1', productName: 'Rooster', sellerId: 'seller-1', price: 150, quantity: 2, totalPrice: 300 },
    ],
    paymentMethod: PaymentMethod.CASH_ON_DELIVERY,
  };

  function fillValidAddress(): void {
    component.addressForm.setValue({
      fullName: 'John Doe',
      phoneNumber: '+212612345678',
      city: 'Casablanca',
      address: '12 rue des Fleurs',
    });
  }

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [Checkout, HttpClientTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    cartService = TestBed.inject(CartService);
    router = TestBed.inject(Router);
    httpTesting = TestBed.inject(HttpTestingController);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('redirects to /cart when the cart is empty', () => {
    fixture = TestBed.createComponent(Checkout);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/cart']);
  });

  describe('with items in the cart', () => {
    beforeEach(() => {
      cartService.add(mockProduct, 2);
      fixture = TestBed.createComponent(Checkout);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('starts on the address step', () => {
      expect(component.step()).toBe('address');
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('continueFromAddress() blocks when the form is invalid', () => {
      component.continueFromAddress();

      expect(component.step()).toBe('address');
      expect(component.addressForm.touched).toBe(true);
    });

    it('continueFromAddress() advances to review when the form is valid', () => {
      fillValidAddress();
      component.continueFromAddress();

      expect(component.step()).toBe('review');
    });

    it('review step exposes cart items and totals', () => {
      fillValidAddress();
      component.continueFromAddress();

      expect(component.items()).toHaveLength(1);
      expect(component.subtotal()).toBe(300);
      expect(component.delivery).toBe(0);
      expect(component.total()).toBe(300);
    });

    it('backToAddress() returns to the address step', () => {
      fillValidAddress();
      component.continueFromAddress();
      component.backToAddress();

      expect(component.step()).toBe('address');
    });

    it('continueFromReview() advances to the payment step', () => {
      fillValidAddress();
      component.continueFromAddress();
      component.continueFromReview();

      expect(component.step()).toBe('payment');
    });

    it('renders Pay on Delivery as the only, pre-selected payment option', () => {
      fillValidAddress();
      component.continueFromAddress();
      component.continueFromReview();
      fixture.detectChanges();

      const radio = fixture.nativeElement.querySelector('.payment-option input[type="radio"]');
      expect(radio.checked).toBe(true);
      expect(radio.disabled).toBe(true);
      expect(fixture.nativeElement.textContent).toContain('Pay on Delivery');
    });

    it('placeOrder() creates the order, clears the cart and navigates to the order details page', () => {
      fillValidAddress();
      component.continueFromAddress();
      component.continueFromReview();

      component.placeOrder();

      const req = httpTesting.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        fullName: 'John Doe',
        phoneNumber: '+212612345678',
        city: 'Casablanca',
        address: '12 rue des Fleurs',
        items: [{ productId: 'prod-1', productName: 'Rooster', price: 150, quantity: 2 }],
      });

      req.flush(mockOrder);

      expect(cartService.items()).toEqual([]);
      expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-1']);
    });

    it('placeOrder() does not clear the cart when order creation fails', () => {
      fillValidAddress();
      component.continueFromAddress();
      component.continueFromReview();

      component.placeOrder();

      const req = httpTesting.expectOne(apiUrl);
      req.flush('Insufficient stock', { status: 409, statusText: 'Conflict' });

      expect(cartService.items()).toHaveLength(1);
      expect(component.submitting()).toBe(false);
      expect(router.navigate).not.toHaveBeenCalledWith(['/orders', 'order-1']);
    });

    it('placeOrder() prevents duplicate submission while a request is in flight', () => {
      fillValidAddress();
      component.continueFromAddress();
      component.continueFromReview();

      component.placeOrder();
      component.placeOrder();

      httpTesting.expectOne(apiUrl).flush(mockOrder);
    });
  });
});
