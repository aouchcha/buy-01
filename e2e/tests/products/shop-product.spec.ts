// import { test, expect } from '@playwright/test';

// test('user can buy a product from the shop', async ({ page }) => {
//   await page.goto('/');

//   // =========================
//   // Login
//   // =========================

//   await page.getByRole('link', { name: 'Login' }).click();

//   await page
//     .getByRole('textbox', { name: 'you@example.com' })
//     .fill(process.env.E2E_EMAIL_BUYER!);

//   await page
//     .getByRole('textbox', { name: 'Password' })
//     .fill(process.env.E2E_PASSWORD!);

//   await page.getByRole('button', { name: 'Sign In' }).click();

//   // Verify login
//   await expect(
//     page.getByRole('link', { name: 'Profile' })
//   ).toBeVisible();

//   // =========================
//   // Check products
//   // =========================

//   const addToCartButton = page.getByRole('button', {
//     name: 'Add to cart',
//   });

//   const productCount = await addToCartButton.count();

//   // No products available
//   if (productCount === 0) {
//     console.log('No products available. Skipping purchase flow.');


//     await expect(
//       page.getByText(/No birds available right now/i)
//     ).toBeVisible();

//     return;
//   }

//   // =========================
//   // Add first product to cart
//   // =========================

//   await addToCartButton.first().click();

//   // =========================
//   // Cart
//   // =========================

//   await page.getByRole('link', { name: 'Cart' }).click();

//   await expect(page).toHaveURL(/cart/);

//   // =========================
//   // Checkout
//   // =========================

//   await page
//     .getByRole('button', { name: 'Proceed to Checkout' })
//     .click();

//   // =========================
//   // Shipping information
//   // =========================

//   await page
//     .getByRole('textbox', { name: 'Full name' })
//     .fill('E2E Test User');

//   await page
//     .getByRole('textbox', { name: 'Phone' })
//     .fill('+212600000000');

//   await page
//     .getByRole('textbox', { name: 'City' })
//     .fill('Oujda');

//   await page
//     .getByRole('textbox', { name: 'Postal code' })
//     .fill('60000');

//   await page
//     .getByRole('textbox', { name: 'Address' })
//     .fill('E2E Test Address');

//   await page.getByRole('button', { name: 'Continue' }).click();

//   await page.getByRole('button', { name: 'Continue' }).click();

//   // =========================
//   // Place order
//   // =========================

//   await page.getByRole('button', { name: 'Place Order' }).click();

//   // TODO: Replace this with your actual order confirmation.
//   await expect(
//     page.getByText(/order/i).first()
//   ).toBeVisible();

//   // =========================
//   // Continue shopping
//   // =========================

//   await page
//     .getByRole('link', { name: 'Continue shopping' })
//     .click();

//   await expect(page).toHaveURL(/\/$/);
// });