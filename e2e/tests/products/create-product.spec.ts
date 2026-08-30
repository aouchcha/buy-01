// import { test, expect } from '@playwright/test';
// import path from 'path';

// test('seller can create a product with an image', async ({ page }) => {
//   await page.goto('/');

//   // Login
//   await page.getByRole('link', { name: 'Login' }).click();

//   await page
//     .getByRole('textbox', { name: 'you@example.com' })
//     .fill(process.env.E2E_EMAIL!);

//   await page
//     .getByRole('textbox', { name: 'Password' })
//     .fill(process.env.E2E_PASSWORD!);

//   await page.getByRole('button', { name: 'Sign In' }).click();

//   // Open seller dashboard
//   await page.getByRole('link', { name: 'Dashboard' }).click();

//   // Open create product form
//   await page.getByRole('button', { name: '+ Add product' }).click();

//   // Create unique product data
//   const productName = `E2E Chicken ${Date.now()}`;

//   await page
//     .getByRole('textbox', { name: 'Name' })
//     .fill(productName);

//   await page
//     .getByRole('textbox', { name: 'Description' })
//     .fill('Good chicken created by E2E test');

//   await page
//     .getByRole('spinbutton', { name: 'Price (MAD)' })
//     .fill('30');

//   await page
//     .getByRole('spinbutton', { name: 'Quantity' })
//     .fill('300');

//   await page
//     .getByLabel('Category')
//     .selectOption('1: LIVE_POULTRY');

//   // Upload product image
//   const imagePath = path.join(
//     process.cwd(),
//     'tests',
//     'fixtures',
//     'product-image.jpg.jpg'
//   );

//   await page
//     .getByLabel('add_photo_alternate Add image')
//     .setInputFiles(imagePath);

//   // Create product
//   await page
//     .getByRole('button', { name: 'Add product', exact: true })
//     .click();

//   // Verify product was created
//   await expect(
//     page.getByText(productName, { exact: true })
//   ).toBeVisible();
// });
