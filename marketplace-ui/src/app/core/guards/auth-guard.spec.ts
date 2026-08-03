import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { authGuard } from './auth-guard';

describe('authGuard', () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: Router,
          useValue: {
            createUrlTree: vi.fn((commands: string[]) => ({ commands } as unknown as UrlTree)),
            navigate: vi.fn().mockResolvedValue(true),
          },
        },
      ],
    });

    router = TestBed.inject(Router);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should return true when token exists in localStorage', () => {
    localStorage.setItem('token', 'valid-token-123');

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    expect(result).toBe(true);
  });

  it('should redirect to /login when no token', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).not.toBe(true);
  });

  it('should redirect to /login when token is empty string', () => {
    localStorage.setItem('token', '');

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, {} as any)
    );

    expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).not.toBe(true);
  });
});
