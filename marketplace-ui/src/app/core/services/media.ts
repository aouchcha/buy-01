import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MediaResponse } from '../models/media';
import { environment } from '../../../environments/environment';


export const MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB, matches backend limit

@Injectable({
  providedIn: 'root',
})
export class Media {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/media/images`;

  uploadImage(
    userId: string,
    productId: string | null,
    files: File[],
    type: string
  ): Observable<MediaResponse> {

    const formData = new FormData();

    for (const file of files) {
      formData.append('pictures', file);
    }

    formData.append('userId', userId);
    formData.append('type', type);

    if (productId) {
      formData.append('productId', productId);
    }

    return this.http.post<MediaResponse>(this.apiUrl, formData);
  }


  updateImages(
    userId: string,
    productId: string | null,
    deletedUrls: string[],
    newImages: File[],
    type: string
  ): Observable<MediaResponse[]> {

    const formData = new FormData();

    for (const url of deletedUrls) {
      formData.append('deletedUrls', url);
    }

    // if (formData['deletedUrls'])

    for (const file of newImages) {
      formData.append('newImages', file);
    }

    formData.append('userId', userId);
    formData.append('type', type);

    if (productId) {
      formData.append('productId', productId);
    }

    return this.http.put<MediaResponse[]>(this.apiUrl, formData);
  }


  getMyImages(userId: string): Observable<MediaResponse> {
    return this.http.get<MediaResponse>(`${this.apiUrl}?userId=${userId}`);
  }

  deleteImage(mediaId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${mediaId}`);
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