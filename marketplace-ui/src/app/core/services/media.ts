import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MediaResponse } from '../models/media';


export const MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB, matches backend limit?

@Injectable({
  providedIn: 'root',
})
export class Media {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/media/images';

  uploadImage(productId: number, file: File): Observable<MediaResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('productId', productId.toString());

    return this.http.post<MediaResponse>(this.apiUrl, formData);
  }

  validateImage(file: File): string | null {
    if (!file.type.startsWith('image/')) {
      return 'Only image files are allowed.';
    }
    if (file.size > MAX_IMAGE_SIZE_BYTES) {
      return 'Image must be 2MB or smaller.';
    }
    return null;
  }
}