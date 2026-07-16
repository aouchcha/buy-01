import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ProductDto, ProductRequest } from '../models/product';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class Product {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/product`;

  getAll(): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(this.apiUrl);
  }

  getById(id: string): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.apiUrl}/${id}`);
  }

  getMyProducts(): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(this.apiUrl + '/myProducts');
  }

  create(request: ProductRequest): Observable<ProductDto> {
    return this.http.post<ProductDto>(this.apiUrl, request);
  }

  update(id: string, request: ProductRequest): Observable<ProductDto> {
    return this.http.put<ProductDto>(`${this.apiUrl}/${id}`, request);
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}