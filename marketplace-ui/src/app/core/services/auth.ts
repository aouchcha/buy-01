import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { LoginRequest } from '../models/auth-request';
import { Observable } from 'rxjs';
import { AuthResponse } from '../models/auth-response';


@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/auth';

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/login`,
      data
    );
  }


  register(data: FormData) : Observable<AuthResponse>{
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/signup`,
      data
    )
  }

}
