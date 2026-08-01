import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { noAuthGuard } from './no-auth-guard';

describe('noAuthGuard', () => {
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

  it('should return true when no token in localStorage', () => {
    const result = TestBed.runInInjectionContext(() =>
      noAuthGuard({} as any, {} as any)
    );

    expect(result).toBe(true);
  });

  it('should redirect to / when token exists', () => {
    localStorage.setItem('token', 'existing-token-abc');

    const result = TestBed.runInInjectionContext(() =>
      noAuthGuard({} as any, {} as any)
    );

    expect(router.createUrlTree).toHaveBeenCalledWith(['/']);
    expect(result).not.toBe(true);
  });
});
