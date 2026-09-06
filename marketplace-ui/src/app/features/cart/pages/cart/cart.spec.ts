import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { Cart } from './cart';
import { CartService } from '../../../../core/services/cart';
import { Cart as CartModel } from '../../../../core/models/cart';
import { environment } from '../../../../../environments/environment';

describe('Cart', () => {
  let component: Cart;
  let fixture: ComponentFixture<Cart>;
  let cartService: CartService;
  let router: Router;
  let httpTesting: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/cart`;

  const mockCart: CartModel = {
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

  function createComponentAndLoad(cart: CartModel = mockCart): void {
    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpTesting.expectOne(apiUrl).flush(cart);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Cart, MatDialogModule, HttpClientTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    cartService = TestBed.inject(CartService);
    router = TestBed.inject(Router);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.clearAllMocks();
  });

  it('should create', () => {
    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    fixture.detectChanges();
    httpTesting.expectOne(apiUrl).flush({ id: 'cart-1', userId: 'user-1', cartItems: [] });

    expect(component).toBeTruthy();
  });

  it('shows an empty cart when the server cart has no items', () => {
    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    fixture.detectChanges();
    httpTesting.expectOne(apiUrl).flush({ id: 'cart-1', userId: 'user-1', cartItems: [] });

    expect(component.items()).toEqual([]);
    expect(component.total()).toBe(0);
  });

  it('loads items from the backend cart on init', () => {
    createComponentAndLoad();

    expect(component.items()).toHaveLength(1);
    expect(component.total()).toBe(300);
  });

  it('increment() PATCHes the cart with quantity + 1', () => {
    createComponentAndLoad();

    component.increment('prod-1', 2);

    const req = httpTesting.expectOne(`${apiUrl}/items`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ productId: 'prod-1', quantity: 3 });
    req.flush({ ...mockCart, cartItems: [{ ...mockCart.cartItems[0], quantity: 3, totalPrice: 450 }] });

    expect(cartService.items()[0].quantity).toBe(3);
  });

  it('decrement() PATCHes the cart with quantity - 1 when above 1', () => {
    createComponentAndLoad();

    component.decrement('prod-1', 2);

    const req = httpTesting.expectOne(`${apiUrl}/items`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ productId: 'prod-1', quantity: 1 });
    req.flush({ ...mockCart, cartItems: [{ ...mockCart.cartItems[0], quantity: 1, totalPrice: 150 }] });

    expect(cartService.items()[0].quantity).toBe(1);
  });

  it('decrement() removes the item instead of going to 0', () => {
    createComponentAndLoad();

    component.decrement('prod-1', 1);

    const req = httpTesting.expectOne(`${apiUrl}/items/prod-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ ...mockCart, cartItems: [] });

    expect(cartService.items()).toEqual([]);
  });

  it('remove() takes the item out of the cart', () => {
    createComponentAndLoad();

    component.remove('prod-1');

    const req = httpTesting.expectOne(`${apiUrl}/items/prod-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ ...mockCart, cartItems: [] });

    expect(cartService.items()).toEqual([]);
  });

  it('does not render the Proceed to Checkout button when the cart is empty', () => {
    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    fixture.detectChanges();
    httpTesting.expectOne(apiUrl).flush({ id: 'cart-1', userId: 'user-1', cartItems: [] });
    fixture.detectChanges();

    const button: HTMLButtonElement | null = fixture.nativeElement.querySelector('.btn-checkout');
    expect(button).toBeNull();
  });

  it('enables Proceed to Checkout and navigates to /checkout when there are items', () => {
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    createComponentAndLoad();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-checkout');
    expect(button.disabled).toBe(false);

    component.goToCheckout();

    expect(router.navigate).toHaveBeenCalledWith(['/checkout']);
  });

  it('goToCheckout() does nothing when the cart is empty', () => {
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    fixture.detectChanges();
    httpTesting.expectOne(apiUrl).flush({ id: 'cart-1', userId: 'user-1', cartItems: [] });

    component.goToCheckout();

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
