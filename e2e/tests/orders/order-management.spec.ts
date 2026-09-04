import { test, expect } from '@playwright/test';
import path from 'path';

test.use({
  ignoreHTTPSErrors: true,
});

// =========================================================
// SHARED TEST DATA
// =========================================================

const sellerEmail = process.env.E2E_EMAIL;
const sellerPassword = process.env.E2E_PASSWORD;

const buyerEmail = process.env.E2E_EMAIL_BUYER;
const buyerPassword = process.env.E2E_PASSWORD_BUYER;

if (!sellerEmail || !sellerPassword || !buyerEmail || !buyerPassword) {
  throw new Error(
    'Missing E2E_EMAIL, E2E_PASSWORD, E2E_EMAIL_BUYER or E2E_PASSWORD_BUYER in e2e/.env'
  );
}

let productName: string;
let orderId: string;

test.describe.configure({
  mode: 'serial',
});

// =========================================================
// TEST 1 — SETUP: SELLER CREATES A PRODUCT
// =========================================================

test('setup: seller creates a product', async ({ page }) => {
  productName = `E2E Order ${Date.now()}`;

  await page.goto('/');
  await page.getByRole('link', { name: 'Login' }).click();

  await page
    .getByRole('textbox', { name: 'you@example.com' })
    .fill(sellerEmail);
  await page.getByRole('textbox', { name: 'Password' }).fill(sellerPassword);
  await page.getByRole('button', { name: 'Sign In' }).click();

  await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
  await page.getByRole('link', { name: 'Dashboard' }).click();

  await page.getByRole('button', { name: '+ Add product' }).click();

  await page.getByRole('textbox', { name: 'Name' }).fill(productName);
  await page
    .getByRole('textbox', { name: 'Description' })
    .fill('Product created by order-management E2E test');
  await page.getByRole('spinbutton', { name: 'Price (MAD)' }).fill('40');
  await page.getByRole('spinbutton', { name: 'Quantity' }).fill('50');
  await page.getByLabel('Category').selectOption('1: LIVE_POULTRY');

  const imagePath = path.join(
    process.cwd(),
    'tests',
    'fixtures',
    'product-image.jpg.jpg'
  );
  await page
    .getByLabel('add_photo_alternate Add image')
    .setInputFiles(imagePath);

  await page
    .getByRole('button', { name: 'Add product', exact: true })
    .click();

  await expect(page.getByText(productName, { exact: true })).toBeVisible();
});

// =========================================================
// TEST 2 — BUYER PURCHASES THE PRODUCT
// =========================================================

test('buyer purchases the product and lands on order details', async ({ page }) => {
  expect(productName).toBeTruthy();

  await page.goto('/');
  await page.getByRole('link', { name: 'Login' }).click();

  await page.getByRole('textbox', { name: 'you@example.com' }).fill(buyerEmail);
  await page.getByRole('textbox', { name: 'Password' }).fill(buyerPassword);
  await page.getByRole('button', { name: 'Sign In' }).click();

  await expect(page.getByRole('link', { name: 'Profile' })).toBeVisible();

  const productCard = page
    .locator('article.card')
    .filter({ hasText: productName })
    .first();
  await expect(productCard).toBeVisible();

  await productCard.getByRole('button', { name: 'Add to cart' }).click();

  await page.getByRole('link', { name: 'Cart' }).click();
  await expect(page).toHaveURL(/cart/);

  await page.getByRole('button', { name: 'Proceed to Checkout' }).click();

  await page.getByRole('textbox', { name: 'Full name' }).fill('E2E Order Buyer');
  await page.getByRole('textbox', { name: 'Phone' }).fill('+212600000001');
  await page.getByRole('textbox', { name: 'City' }).fill('Oujda');
  await page.getByRole('textbox', { name: 'Postal code' }).fill('60000');
  await page.getByRole('textbox', { name: 'Address' }).fill('E2E Order Test Address');

  await page.getByRole('button', { name: 'Continue' }).click();
  await page.getByRole('button', { name: 'Continue' }).click();
  await page.getByRole('button', { name: 'Place Order' }).click();

  await expect(page.getByRole('heading', { name: 'Order created successfully' })).toBeVisible();

  const match = page.url().match(/\/orders\/([^/?#]+)/);
  expect(match).not.toBeNull();
  orderId = match![1];

  const statusBadge = page.locator('.status-badge');
  await expect(statusBadge).toBeVisible();
  await expect(statusBadge).toHaveText(/PENDING|CONFIRMED/);

  await expect(
    page.locator('.order-card').getByText(productName, { exact: true })
  ).toBeVisible();
});

// =========================================================
// TEST 3 — ORDER APPEARS IN THE ORDER LIST
// =========================================================

test('order appears in the order list', async ({ page }) => {
  expect(orderId).toBeTruthy();

  await page.goto('/');
  await page.getByRole('link', { name: 'Login' }).click();
  await page.getByRole('textbox', { name: 'you@example.com' }).fill(buyerEmail);
  await page.getByRole('textbox', { name: 'Password' }).fill(buyerPassword);
  await page.getByRole('button', { name: 'Sign In' }).click();

  await page.goto('/orders');

  const orderRow = page
    .locator('a.order-row')
    .filter({ hasText: `Order #${orderId}` });
  await expect(orderRow).toBeVisible();
  await expect(orderRow.locator('.status-badge')).toBeVisible();

  await orderRow.click();
  await expect(page).toHaveURL(new RegExp(`/orders/${orderId}`));
});

// =========================================================
// TEST 4 — BUYER CANCELS THE ORDER
// =========================================================

test('buyer can cancel the order', async ({ page }) => {
  expect(orderId).toBeTruthy();

  await page.goto('/');
  await page.getByRole('link', { name: 'Login' }).click();
  await page.getByRole('textbox', { name: 'you@example.com' }).fill(buyerEmail);
  await page.getByRole('textbox', { name: 'Password' }).fill(buyerPassword);
  await page.getByRole('button', { name: 'Sign In' }).click();

  await page.goto(`/orders/${orderId}`);

  await page.getByRole('button', { name: 'Cancel order' }).click();
  await page
    .locator('.confirm-dialog')
    .getByRole('button', { name: 'Cancel order' })
    .click();

  await expect(page.getByText('Order cancelled.')).toBeVisible();
  await expect(page.locator('.status-badge')).toHaveText('CANCELLED');
});

// =========================================================
// TEST 5 — BUYER DELETES THE CANCELLED ORDER
// =========================================================

test('buyer can delete the cancelled order', async ({ page }) => {
  expect(orderId).toBeTruthy();

  await page.goto('/');
  await page.getByRole('link', { name: 'Login' }).click();
  await page.getByRole('textbox', { name: 'you@example.com' }).fill(buyerEmail);
  await page.getByRole('textbox', { name: 'Password' }).fill(buyerPassword);
  await page.getByRole('button', { name: 'Sign In' }).click();

  await page.goto(`/orders/${orderId}`);

  await page.getByRole('button', { name: 'Delete order' }).click();
  await page
    .locator('.confirm-dialog')
    .getByRole('button', { name: 'Delete order' })
    .click();

  await expect(page).toHaveURL(/\/orders$/);
  await expect(page.getByText('Order deleted.')).toBeVisible();

  await expect(
    page.locator('a.order-row').filter({ hasText: `Order #${orderId}` })
  ).not.toBeVisible();
});
