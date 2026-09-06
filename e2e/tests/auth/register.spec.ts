import { test, expect } from '@playwright/test';

test('user can register as a seller', async ({ page }) => {
  await page.goto('/');

  // Open registration page
  await page.getByRole('link', { name: 'Sign Up' }).click();

  // Generate a unique email for every test run
  const email = `seller-${Date.now()}@example.com`;
  const password = '123456789';

  // Registration form
  await page
    .getByRole('textbox', { name: 'First name' })
    .fill('seller');

  await page
    .getByRole('textbox', { name: 'Last name' })
    .fill('seller');

  await page
    .getByRole('textbox', { name: 'Email' })
    .fill(email);

  await page
    .getByRole('textbox', { name: 'Password', exact: true })
    .fill(password);

  await page
    .getByRole('textbox', { name: 'Confirm password' })
    .fill(password);

  // Select seller account
  await page
    .getByRole('button', { name: 'Sell birds List and manage' })
    .click();

  // Create account
  await page
    .getByRole('button', { name: 'Create account' })
    .click();

  // Registration should redirect to home
  await expect(page).toHaveURL(/\/$/);

  // Authenticated navigation should be displayed
  await expect(
    page.getByRole('link', { name: 'Profile' })
  ).toBeVisible();

  await expect(
    page.getByRole('link', { name: 'Dashboard' })
  ).toBeVisible();

  await expect(
    page.getByText('Logout', { exact: true })
  ).toBeVisible();

  // Login should no longer be displayed
  await expect(
    page.getByRole('link', { name: 'Login' })
  ).not.toBeVisible();
});
