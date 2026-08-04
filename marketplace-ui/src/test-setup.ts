/**
 * Global Vitest setup file (registered via `test.setupFiles` in angular.json).
 *
 * Root cause this file fixes:
 * Starting with Node.js 22.4 (and enabled by default on newer "Current" releases
 * such as Node 25), the runtime exposes an experimental, *non-functional*
 * `globalThis.localStorage` / `globalThis.sessionStorage` (a plain `{}` with no
 * `Storage` methods) whenever `--localstorage-file` is not given a valid path.
 *
 * Vitest's jsdom environment does not overwrite globals that already exist on
 * `globalThis` before jsdom is attached, so on affected Node versions the real,
 * working `Storage` implementation that jsdom would normally provide is shadowed
 * by Node's broken stub. Any component/service/test that calls
 * `localStorage.getItem/setItem/clear/...` then throws
 * `TypeError: localStorage.getItem is not a function`, which in turn leaves the
 * Angular TestBed in a dirty state for subsequent test files
 * ("Cannot configure the test module when the test module has already been
 * instantiated").
 *
 * This installs a small, spec-compliant, in-memory Storage polyfill on
 * `globalThis` (and `window`) before any spec file or the Angular TestBed runs,
 * so behavior is deterministic across Node versions instead of depending on
 * whichever (possibly broken) storage implementation the current Node runtime
 * happens to provide.
 */

class MemoryStorage implements Storage {
  private store = new Map<string, string>();

  get length(): number {
    return this.store.size;
  }

  clear(): void {
    this.store.clear();
  }

  getItem(key: string): string | null {
    return this.store.has(key) ? this.store.get(key)! : null;
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null;
  }

  removeItem(key: string): void {
    this.store.delete(key);
  }

  setItem(key: string, value: string): void {
    this.store.set(key, String(value));
  }
}

function installWorkingStorage(propertyName: 'localStorage' | 'sessionStorage'): void {
  const existing = (globalThis as any)[propertyName];
  const isFunctional = typeof existing?.getItem === 'function' && typeof existing?.setItem === 'function';

  if (isFunctional) {
    // The environment already provides a working Storage implementation
    // (e.g. a Node version without the broken global, or a jsdom instance
    // that was correctly attached) — leave it untouched.
    return;
  }

  const storage = new MemoryStorage();

  for (const target of [globalThis, (globalThis as any).window].filter(Boolean)) {
    Object.defineProperty(target, propertyName, {
      value: storage,
      configurable: true,
      writable: true,
      enumerable: true,
    });
  }
}

installWorkingStorage('localStorage');
installWorkingStorage('sessionStorage');
