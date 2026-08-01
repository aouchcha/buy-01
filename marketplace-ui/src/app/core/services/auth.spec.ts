import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Auth } from './auth';
import { Role, User } from '../models/user';
import { AuthResponse } from '../models/auth-response';
import { environment } from '../../../environments/environment';

describe('Auth service', () => {
  let service: Auth;
  let httpTesting: HttpTestingController;

  const mockUser: User = {
    id: 'user-1',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    role: Role.CLIENT,
  };

  const mockAuthResponse: AuthResponse = {
    token: 'jwt-token-xyz',
    user: mockUser,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });

    service = TestBed.inject(Auth);
    httpTesting = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    httpTesting.verify();
  });

  // --- login() ---

  it('login() should POST and store token + update currentUser$', () => {
    let emittedUser: User | null = undefined as any;
    service.currentUser$.subscribe((u) => (emittedUser = u));

    service.login({ email: 'john@example.com', password: 'secret' }).subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);

    expect(localStorage.getItem('token')).toBe('jwt-token-xyz');
    expect(emittedUser).toEqual(mockUser);
  });

  // --- register() ---

  it('register() should POST and store token + update currentUser$', () => {
    let emittedUser: User | null = undefined as any;
    service.currentUser$.subscribe((u) => (emittedUser = u));

    service
      .register({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: 'secret',
        role: 'CLIENT',
      })
      .subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/auth/signup`);
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);

    expect(localStorage.getItem('token')).toBe('jwt-token-xyz');
    expect(emittedUser).toEqual(mockUser);
  });

  // --- logout() ---

  it('logout() should remove token and set currentUser$ to null', () => {
    localStorage.setItem('token', 'existing-token');
    service['currentUserSubject'].next(mockUser);

    let emittedUser: User | null = undefined as any;
    service.currentUser$.subscribe((u) => (emittedUser = u));

    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(emittedUser).toBeNull();
  });

  // --- isLoggedIn() ---

  it('isLoggedIn() should return true when token exists', () => {
    localStorage.setItem('token', 'some-token');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('isLoggedIn() should return false when no token', () => {
    expect(service.isLoggedIn()).toBe(false);
  });

  // --- getUserRole() ---

  it('getUserRole() should return the role when user is set', () => {
    service['currentUserSubject'].next(mockUser);
    expect(service.getUserRole()).toBe(Role.CLIENT);
  });

  it('getUserRole() should return null when no user', () => {
    service['currentUserSubject'].next(null);
    expect(service.getUserRole()).toBeNull();
  });

  // --- getToken() ---

  it('getToken() should return token from localStorage', () => {
    localStorage.setItem('token', 'stored-token');
    expect(service.getToken()).toBe('stored-token');
  });

  it('getToken() should return null when no token in localStorage', () => {
    expect(service.getToken()).toBeNull();
  });

  // --- loadCurrentUser() ---

  it('loadCurrentUser() should GET /users/me and update currentUser$', () => {
    let emittedUser: User | null = undefined as any;
    service.currentUser$.subscribe((u) => (emittedUser = u));

    service.loadCurrentUser().subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/users/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUser);

    expect(emittedUser).toEqual(mockUser);
  });
});
