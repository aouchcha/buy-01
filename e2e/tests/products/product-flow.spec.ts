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

if (
  !sellerEmail ||
  !sellerPassword ||
  !buyerEmail ||
  !buyerPassword
) {
  throw new Error(
    'Missing E2E_EMAIL, E2E_PASSWORD, E2E_EMAIL_BUYER or E2E_PASSWORD_BUYER in e2e/.env'
  );
}

// Product created by test 1
let productName: string;

// Run the 3 tests in order
test.describe.configure({
  mode: 'serial',
});

// =========================================================
// TEST 1 — CREATE PRODUCT
// =========================================================

test('seller can create a product with an image', async ({ page }) => {
  productName = `E2E Chicken ${Date.now()}`;

  // Login as seller
  await page.goto('/');

  await page.getByRole('link', { name: 'Login' }).click();

  await page
    .getByRole('textbox', { name: 'you@example.com' })
    .fill(sellerEmail);

  await page
    .getByRole('textbox', { name: 'Password' })
    .fill(sellerPassword);

  await page
    .getByRole('button', { name: 'Sign In' })
    .click();

  await expect(
    page.getByRole('link', { name: 'Dashboard' })
  ).toBeVisible();

  // Open dashboard
  await page
    .getByRole('link', { name: 'Dashboard' })
    .click();

  // Open create product form
  await page
    .getByRole('button', { name: '+ Add product' })
    .click();

  // Product information
  await page
    .getByRole('textbox', { name: 'Name' })
    .fill(productName);

  await page
    .getByRole('textbox', { name: 'Description' })
    .fill('Good chicken created by E2E test');

  await page
    .getByRole('spinbutton', { name: 'Price (MAD)' })
    .fill('30');

  await page
    .getByRole('spinbutton', { name: 'Quantity' })
    .fill('300');

  await page
    .getByLabel('Category')
    .selectOption('1: LIVE_POULTRY');

  // Upload image
  const imagePath = path.join(
    process.cwd(),
    'tests',
    'fixtures',
    'product-image.jpg.jpg'
  );

  await page
    .getByLabel('add_photo_alternate Add image')
    .setInputFiles(imagePath);

  // Create product
  await page
    .getByRole('button', {
      name: 'Add product',
      exact: true,
    })
    .click();

  // Verify product was created
  await expect(
    page.getByText(productName, { exact: true })
  ).toBeVisible();

  console.log(`Created product: ${productName}`);
});

// =========================================================
// TEST 2 — BUY PRODUCT
// =========================================================

test('buyer can buy the created product', async ({ page }) => {
  // Make sure test 1 created the product
  expect(productName).toBeTruthy();

  // Login as buyer
  await page.goto('/');

  await page
    .getByRole('link', { name: 'Login' })
    .click();

  await page
    .getByRole('textbox', { name: 'you@example.com' })
    .fill(buyerEmail);

  await page
    .getByRole('textbox', { name: 'Password' })
    .fill(buyerPassword);

  await page
    .getByRole('button', { name: 'Sign In' })
    .click();

  await expect(
    page.getByRole('link', { name: 'Profile' })
  ).toBeVisible();

  // Verify our product is available
  await expect(
    page.getByText(productName, { exact: true })
  ).toBeVisible();

  // Find exact product card
  const productCard = page
    .locator('div')
    .filter({
      hasText: productName,
    })
    .filter({
      has: page.getByRole('button', {
        name: 'Add to cart',
      }),
    })
    .first();

  await expect(productCard).toBeVisible();

  // Add product to cart
  await productCard
    .getByRole('button', {
      name: 'Add to cart',
    })
    .click();

  // Go to cart
  await page
    .getByRole('link', { name: 'Cart' })
    .click();

  await expect(page).toHaveURL(/cart/);

  await expect(
    page.getByText(productName, { exact: true })
  ).toBeVisible();

  // Checkout
  await page
    .getByRole('button', {
      name: 'Proceed to Checkout',
    })
    .click();

  // Shipping information
  await page
    .getByRole('textbox', { name: 'Full name' })
    .fill('E2E Test User');

  await page
    .getByRole('textbox', { name: 'Phone' })
    .fill('+212600000000');

  await page
    .getByRole('textbox', { name: 'City' })
    .fill('Oujda');

  await page
    .getByRole('textbox', { name: 'Postal code' })
    .fill('60000');

  await page
    .getByRole('textbox', { name: 'Address' })
    .fill('E2E Test Address');

  // Checkout step 1
  await page
    .getByRole('button', { name: 'Continue' })
    .click();

  // Checkout step 2
  await page
    .getByRole('button', { name: 'Continue' })
    .click();

  // Place order
  await page
    .getByRole('button', { name: 'Place Order' })
    .click();

  // Verify order
  await expect(
    page.getByText(/order/i).first()
  ).toBeVisible();

  console.log(`Purchased product: ${productName}`);
});

// =========================================================
// TEST 3 — DELETE PRODUCT
// =========================================================

test('seller can delete the created product', async ({ page }) => {
  // Make sure previous tests created the product
  expect(productName).toBeTruthy();

  // Login as seller
  await page.goto('/');

  await page
    .getByRole('link', { name: 'Login' })
    .click();

  await page
    .getByRole('textbox', { name: 'you@example.com' })
    .fill(sellerEmail);

  await page
    .getByRole('textbox', { name: 'Password' })
    .fill(sellerPassword);

  await page
    .getByRole('button', { name: 'Sign In' })
    .click();

  await expect(
    page.getByRole('link', { name: 'Dashboard' })
  ).toBeVisible();

  // Open dashboard
  await page
    .getByRole('link', { name: 'Dashboard' })
    .click();

  // Find exact product
  const createdProduct = page
    .locator('div')
    .filter({
      hasText: productName,
    })
    .first();

  await expect(createdProduct).toBeVisible();

  // Click delete
  await createdProduct
    .getByRole('button')
    .filter({
      hasText: 'delete',
    })
    .click();

  // Confirm deletion
  await page
    .getByRole('button', {
      name: 'Delete',
    })
    .click();

  // Verify product was deleted
  await expect(
    page.getByText(productName, {
      exact: true,
    })
  ).not.toBeVisible();

  console.log(`Deleted product: ${productName}`);
});