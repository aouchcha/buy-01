import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../../core/services/auth';
import { Product } from '../../../../core/services/product';
import { ProductDto } from '../../../../core/models/product';
import { Media } from '../../../../core/services/media';
import { Navbar } from '../../../../layout/navbar/navbar';
import { ConfirmService } from '../../../../core/services/confirm';
import { ToastService } from '../../../../core/services/toast.service';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, Navbar, MatIconModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly auth = inject(Auth);
  private readonly productService = inject(Product);
  private readonly mediaService = inject(Media);
  private readonly fb = inject(FormBuilder);
  private readonly confirmService = inject(ConfirmService);
  private readonly toast = inject(ToastService);

  readonly seller = this.auth.getCurrentUser();

  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal<boolean>(true);
  readonly deletingId = signal<number | null>(null);

  // --- Add product modal state ---
  readonly showAddModal = signal(false);
  readonly submitting = signal(false);
  readonly selectedFile = signal<File | null>(null);
  readonly imagePreviewUrl = signal<string | null>(null);
  readonly fileError = signal<string | null>(null);

  readonly addProductForm = this.fb.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  readonly totalValue = computed(() =>
    this.products().reduce((sum, p) => sum + p.price, 0)
  );

  readonly withImages = computed(
    () => this.products().filter((p) => p.imageUrl).length
  );

  ngOnInit(): void {
    this.fetchProducts();
  }

  private fetchProducts(): void {
    this.loading.set(true);
    this.productService.getMyProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  deleteProduct(id: number): void {
    this.confirmService
      .open({
        title: 'Delete this product?',
        message: 'This cannot be undone. The product and its image will be removed from your catalog.',
        confirmText: 'Delete',
        cancelText: 'Cancel',
        danger: true,
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.deletingId.set(id);

        this.productService.deleteProduct(id).subscribe({
          next: () => {
            this.products.update((list) => list.filter((p) => p.id !== id));
            this.deletingId.set(null);
            this.toast.success('Product deleted.');
          },
          error: () => {
            this.deletingId.set(null);
            this.toast.error('Unable to delete product.');
          },
        });
      });
  }

  // --- Add product modal ---

  openAddModal(): void {
    this.showAddModal.set(true);
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
    this.addProductForm.reset();
    this.selectedFile.set(null);
    this.imagePreviewUrl.set(null);
    this.fileError.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.selectedFile.set(null);
      this.imagePreviewUrl.set(null);
      this.fileError.set(null);
      return;
    }

    const error = this.mediaService.validateImage(file);
    if (error) {
      this.fileError.set(error);
      this.selectedFile.set(null);
      this.imagePreviewUrl.set(null);
      input.value = '';
      return;
    }

    this.fileError.set(null);
    this.selectedFile.set(file);
    this.imagePreviewUrl.set(URL.createObjectURL(file));
  }

  removeSelectedImage(): void {
    this.selectedFile.set(null);
    this.imagePreviewUrl.set(null);
    this.fileError.set(null);
  }

  submitAddProduct(): void {
    if (this.addProductForm.invalid) {
      this.addProductForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { name, description, price, quantity } = this.addProductForm.getRawValue();

    this.productService
      .create({
        name: name!,
        description: description!,
        price: price!,
        quantity: quantity!,
      })
      .subscribe({
        next: (product) => {
          const file = this.selectedFile();
          if (!file) {
            this.products.update((list) => [product, ...list]);
            this.submitting.set(false);
            this.closeAddModal();
            return;
          }

          this.mediaService.uploadImage(product.id, file).subscribe({
            next: (media) => {
              this.products.update((list) => [{ ...product, imageUrl: media.url }, ...list]);
              this.submitting.set(false);
              this.closeAddModal();
            },
            error: () => {
              // Product was created but the image upload failed — keep it visible without an image.
              this.products.update((list) => [product, ...list]);
              this.submitting.set(false);
              this.closeAddModal();
            },
          });
        },
        error: () => this.submitting.set(false),
      });
  }
}