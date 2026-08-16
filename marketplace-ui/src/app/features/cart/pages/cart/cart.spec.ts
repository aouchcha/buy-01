import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';

import { Cart } from './cart';
import { CartService } from '../../../../core/services/cart';
import { ProductDto } from '../../../../core/models/product';

describe('Cart', () => {
  let component: Cart;
  let fixture: ComponentFixture<Cart>;
  let cartService: CartService;
  let router: Router;

  const mockProduct: ProductDto = {
    id: 'prod-1',
    name: 'Rooster',
    description: 'A fine rooster',
    price: 150,
    quantity: 5,
    userId: 'seller-1',
    imageUrls: ['rooster.jpg'],
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [Cart, MatDialogModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    cartService = TestBed.inject(CartService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows an empty cart by default', () => {
    expect(component.items()).toEqual([]);
    expect(component.total()).toBe(0);
  });

  it('reflects items added through the CartService', () => {
    cartService.add(mockProduct, 2);
    fixture.detectChanges();

    expect(component.items()).toHaveLength(1);
    expect(component.total()).toBe(300);
  });

  it('increment() raises the quantity up to the stock limit', () => {
    cartService.add(mockProduct, 1);

    component.increment('prod-1', 1, 5);

    expect(cartService.items()[0].quantity).toBe(2);
  });

  it('increment() does nothing once the stock limit is reached', () => {
    cartService.add(mockProduct, 5);

    component.increment('prod-1', 5, 5);

    expect(cartService.items()[0].quantity).toBe(5);
  });

  it('decrement() lowers the quantity', () => {
    cartService.add(mockProduct, 2);

    component.decrement('prod-1', 2);

    expect(cartService.items()[0].quantity).toBe(1);
  });

  it('remove() takes the item out of the cart', () => {
    cartService.add(mockProduct, 1);

    component.remove('prod-1');

    expect(cartService.items()).toEqual([]);
  });

  it('shows the order summary with subtotal, delivery and total', () => {
    cartService.add(mockProduct, 2);
    fixture.detectChanges();

    expect(component.delivery).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Order Summary');
    expect(fixture.nativeElement.textContent).toContain('Subtotal');
    expect(fixture.nativeElement.textContent).toContain('Delivery');
  });

  it('disables the Proceed to Checkout button when the cart is empty', () => {
    fixture.detectChanges();

    expect(component.items()).toEqual([]);
    const button: HTMLButtonElement | null = fixture.nativeElement.querySelector('.btn-checkout');
    expect(button).toBeNull();
  });

  it('enables Proceed to Checkout and navigates to /checkout when there are items', () => {
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    cartService.add(mockProduct, 1);
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-checkout');
    expect(button.disabled).toBe(false);

    component.goToCheckout();

    expect(router.navigate).toHaveBeenCalledWith(['/checkout']);
  });

  it('goToCheckout() does nothing when the cart is empty', () => {
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();

    component.goToCheckout();

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
