import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Product } from './product';
import { Category, ProductDto, ProductRequest } from '../models/product';
import { environment } from '../../../environments/environment';

describe('Product service', () => {
  let service: Product;
  let httpTesting: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/product`;

  const mockProduct: ProductDto = {
    id: 'prod-1',
    name: 'Laptop',
    description: 'A great laptop',
    price: 1200,
    quantity: 5,
    userId: 'user-1',
    category: Category.FEED_AND_SUPPLIES,
    imageUrls: [],
  };

  const mockRequest: ProductRequest = {
    name: 'Laptop',
    description: 'A great laptop',
    price: 1200,
    quantity: 5,
    category: Category.FEED_AND_SUPPLIES,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });

    service = TestBed.inject(Product);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('getAll() should GET /product', () => {
    let result: ProductDto[] | undefined;

    service.getAll().subscribe((data) => (result = data));

    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush([mockProduct]);

    expect(result).toEqual([mockProduct]);
  });

  it('getById() should GET /product/{id}', () => {
    let result: ProductDto | undefined;

    service.getById('prod-1').subscribe((data) => (result = data));

    const req = httpTesting.expectOne(`${apiUrl}/prod-1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProduct);

    expect(result).toEqual(mockProduct);
  });

  it('getMyProducts() should GET /product/myProducts', () => {
    let result: ProductDto[] | undefined;

    service.getMyProducts().subscribe((data) => (result = data));

    const req = httpTesting.expectOne(`${apiUrl}/myProducts`);
    expect(req.request.method).toBe('GET');
    req.flush([mockProduct]);

    expect(result).toEqual([mockProduct]);
  });

  it('create() should POST to /product with request body', () => {
    let result: ProductDto | undefined;

    service.create(mockRequest).subscribe((data) => (result = data));

    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRequest);
    req.flush(mockProduct);

    expect(result).toEqual(mockProduct);
  });

  it('update() should PUT to /product/{id} with request body', () => {
    let result: ProductDto | undefined;

    service.update('prod-1', mockRequest).subscribe((data) => (result = data));

    const req = httpTesting.expectOne(`${apiUrl}/prod-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(mockRequest);
    req.flush(mockProduct);

    expect(result).toEqual(mockProduct);
  });

  it('deleteProduct() should DELETE /product/{id}', () => {
    let completed = false;

    service.deleteProduct('prod-1').subscribe(() => (completed = true));

    const req = httpTesting.expectOne(`${apiUrl}/prod-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(completed).toBe(true);
  });
});
