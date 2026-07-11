import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Product as ProductModel } from '../models/product';

@Injectable({
  providedIn: 'root',
})
export class Product {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/product';

  getAll(): Observable<ProductModel[]> {
    return this.http.get<ProductModel[]>(this.apiUrl);
  }

  getById(id: number): Observable<ProductModel> {
    return this.http.get<ProductModel>(`${this.apiUrl}/${id}`);
  }
}