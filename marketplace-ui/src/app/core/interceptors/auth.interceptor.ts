// auth.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Auth } from '../services/auth'
import { ToastService } from '../services/toast.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(Auth);
  const router = inject(Router);
  const toast = inject(ToastService);
  const token = authService.getToken();

  const isAuthUrl = req.url.includes('/auth/login') || req.url.includes('/auth/register');

  const authReq = (token && !isAuthUrl)
    ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.logout();
        toast.error('Your session has expired. Please sign in again.');
        router.navigate(['/login']);
      } else if (error.status === 403) {
        toast.error("You don't have permission to do that.");
        router.navigate(['/unauthorized']);
      } else if (error.status === 0) {
        toast.error('Unable to reach the server. Check your connection.');
      } else if (error.status >= 500) {
        toast.error('Something went wrong on our end. Please try again later.');
      }
      return throwError(() => error);
    })
  );
};