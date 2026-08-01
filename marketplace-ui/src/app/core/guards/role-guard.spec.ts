import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { Auth } from '../services/auth';
import { Role } from '../models/user';
import { roleGuard } from './role-guard';

describe('roleGuard', () => {
  let authService: Auth;
  let router: Router;

  const mockRoute = (roles?: string[]): ActivatedRouteSnapshot => ({
    data: { roles },
  } as unknown as ActivatedRouteSnapshot);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: Auth,
          useValue: {
            isLoggedIn: vi.fn(),
            getUserRole: vi.fn(),
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: vi.fn().mockResolvedValue(true),
          },
        },
      ],
    });

    authService = TestBed.inject(Auth);
    router = TestBed.inject(Router);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('should return true when logged in and user has required role', () => {
    vi.spyOn(authService, 'isLoggedIn').mockReturnValue(true);
    vi.spyOn(authService, 'getUserRole').mockReturnValue(Role.SELLER);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(mockRoute([Role.SELLER]), {} as any)
    );

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should navigate to /login and return false when not logged in', () => {
    vi.spyOn(authService, 'isLoggedIn').mockReturnValue(false);
    vi.spyOn(authService, 'getUserRole').mockReturnValue(null);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(mockRoute([Role.SELLER]), {} as any)
    );

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should navigate to /unauthorized and return false when user has wrong role', () => {
    vi.spyOn(authService, 'isLoggedIn').mockReturnValue(true);
    vi.spyOn(authService, 'getUserRole').mockReturnValue(Role.CLIENT);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(mockRoute([Role.SELLER]), {} as any)
    );

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/unauthorized']);
  });

  it('should navigate to /unauthorized and return false when userRole is null', () => {
    vi.spyOn(authService, 'isLoggedIn').mockReturnValue(true);
    vi.spyOn(authService, 'getUserRole').mockReturnValue(null);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(mockRoute([Role.SELLER]), {} as any)
    );

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/unauthorized']);
  });
});
