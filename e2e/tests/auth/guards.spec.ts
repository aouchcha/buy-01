import { test, expect } from '@playwright/test';

test.use({
  ignoreHTTPSErrors: true,
});

const buyerEmail = process.env.E2E_EMAIL_BUYER;
const buyerPassword = process.env.E2E_PASSWORD_BUYER;

if (!buyerEmail || !buyerPassword) {
  throw new Error(
    'Missing E2E_EMAIL_BUYER or E2E_PASSWORD_BUYER in e2e/.env'
  );
}

async function loginAsBuyer(page: import('@playwright/test').Page) {
  await page.goto('/');
  await page.getByRole('link', { name: 'Login' }).click();
  await page
    .getByRole('textbox', { name: 'you@example.com' })
    .fill(buyerEmail!);
  await page
    .getByRole('textbox', { name: 'Password' })
    .fill(buyerPassword!);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page).toHaveURL(/\/$/);
}

// =========================================================
// TEST 1 — authGuard: anonymous user redirected to /login
// =========================================================

test('anonymous user is redirected to login from protected routes', async ({ page }) => {
  const protectedRoutes = ['/checkout', '/orders', '/orders/some-id', '/profile'];

  for (const route of protectedRoutes) {
    await page.goto(route);
    await expect(page).toHaveURL(/\/login$/);
  }
});

// =========================================================
// TEST 2 — roleGuard: buyer redirected to /unauthorized
// =========================================================

test('buyer is redirected to /unauthorized on seller-only routes', async ({ page }) => {
  await loginAsBuyer(page);

  await page.goto('/seller');
  await expect(page).toHaveURL(/unauthorized/);
  await expect(page.getByRole('heading', { name: 'Access denied' })).toBeVisible();

  await page.goto('/seller/media');
  await expect(page).toHaveURL(/unauthorized/);
  await expect(page.getByRole('heading', { name: 'Access denied' })).toBeVisible();
});

// =========================================================
// TEST 3 — noAuthGuard: logged-in user redirected home
// =========================================================

test('logged-in user is redirected home from login/register', async ({ page }) => {
  await loginAsBuyer(page);

  await page.goto('/login');
  await expect(page).toHaveURL(/\/$/);

  await page.goto('/register');
  await expect(page).toHaveURL(/\/$/);
});
