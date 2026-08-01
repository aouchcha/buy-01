import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { Auth } from '../services/auth';
import { ToastService } from '../services/toast.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: { getToken: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { error: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authService = {
      getToken: vi.fn().mockReturnValue(null),
      logout: vi.fn(),
    };
    router = {
      navigate: vi.fn().mockResolvedValue(true),
    };
    toast = {
      error: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Auth, useValue: authService },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.clearAllMocks();
  });

  // --- Authorization header ---

  it('should add Authorization header when token exists', () => {
    authService.getToken.mockReturnValue('my-jwt-token');

    httpClient.get('/api/products').subscribe();

    const req = httpTesting.expectOne('/api/products');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt-token');
    req.flush({});
  });

  it('should NOT add Authorization header when no token', () => {
    authService.getToken.mockReturnValue(null);

    httpClient.get('/api/products').subscribe();

    const req = httpTesting.expectOne('/api/products');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should NOT add Authorization header for /auth/login URL', () => {
    authService.getToken.mockReturnValue('my-jwt-token');

    httpClient.post('/auth/login', {}).subscribe();

    const req = httpTesting.expectOne('/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should NOT add Authorization header for /auth/register URL', () => {
    authService.getToken.mockReturnValue('my-jwt-token');

    httpClient.post('/auth/register', {}).subscribe();

    const req = httpTesting.expectOne('/auth/register');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  // --- Error handling ---

  it('should call logout and navigate to /login on 401', () => {
    authService.getToken.mockReturnValue(null);

    httpClient.get('/api/products').subscribe({ error: () => {} });

    const req = httpTesting.expectOne('/api/products');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authService.logout).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('Your session has expired. Please sign in again.');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should navigate to /unauthorized on 403', () => {
    authService.getToken.mockReturnValue(null);

    httpClient.get('/api/products').subscribe({ error: () => {} });

    const req = httpTesting.expectOne('/api/products');
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });

    expect(toast.error).toHaveBeenCalledWith("You don't have permission to do that.");
    expect(router.navigate).toHaveBeenCalledWith(['/unauthorized']);
  });

  it('should show toast on network error (status 0)', () => {
    authService.getToken.mockReturnValue(null);

    httpClient.get('/api/products').subscribe({ error: () => {} });

    const req = httpTesting.expectOne('/api/products');
    req.flush('Network error', { status: 0, statusText: 'Unknown Error' });

    expect(toast.error).toHaveBeenCalledWith('Unable to reach the server. Check your connection.');
  });

  it('should show toast on 500 server error', () => {
    authService.getToken.mockReturnValue(null);

    httpClient.get('/api/products').subscribe({ error: () => {} });

    const req = httpTesting.expectOne('/api/products');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

    expect(toast.error).toHaveBeenCalledWith('Something went wrong on our end. Please try again later.');
  });
});
