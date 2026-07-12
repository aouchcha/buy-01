import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ProductDto, ProductRequest } from '../models/product';

@Injectable({
  providedIn: 'root',
})
export class Product {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/product';

  getAll(): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(this.apiUrl);
  }

  getById(id: number): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.apiUrl}/${id}`);
  }

  getMyProducts(): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(this.apiUrl + '/myProducts');
  }

  create(request: ProductRequest): Observable<ProductDto> {
    return this.http.post<ProductDto>(this.apiUrl, request);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}