import { test, expect } from '@playwright/test';

test('user can sign in', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('link', { name: 'Login' }).click();

  await page
    .getByRole('textbox', { name: 'you@example.com' })
    .fill(process.env.E2E_EMAIL!);

  await page
    .getByRole('textbox', { name: 'Password' })
    .fill(process.env.E2E_PASSWORD!);

  await page.getByRole('button', { name: 'Sign In' }).click();

  // User is redirected to home
  await expect(page).toHaveURL(/\/$/);

  // Authenticated navigation is displayed
  await expect(
    page.getByRole('link', { name: 'Profile' })
  ).toBeVisible();

  await expect(
    page.getByRole('link', { name: 'Dashboard' })
  ).toBeVisible();

  await expect(
    page.getByText('Logout', { exact: true })
  ).toBeVisible();

  // Login is no longer displayed
  await expect(
    page.getByRole('link', { name: 'Login' })
  ).not.toBeVisible();
});
