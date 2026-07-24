import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
  imports: [CommonModule, RouterModule, ReactiveFormsModule, Navbar, MatIconModule],
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
  readonly deletingId = signal<string | null>(null);

  // --- Add product modal state ---
  readonly showAddModal = signal(false);
  readonly submitting = signal(false);
  readonly selectedFiles = signal<File[]>([]);
  readonly imagePreviewUrls = signal<string[]>([]);
  readonly fileError = signal<string | null>(null);

  readonly addProductForm = this.fb.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  // --- Edit product modal state ---
  readonly showEditModal = signal(false);
  readonly submittingEdit = signal(false);
  readonly editingProduct = signal<ProductDto | null>(null);
  readonly existingImageUrls = signal<string[]>([]);
  readonly selectedEditFiles = signal<File[]>([]);
  readonly selectedEditFilePreviews = signal<string[]>([]);
  readonly editFileError = signal<string | null>(null);

  readonly editProductForm = this.fb.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  readonly totalValue = computed(() =>
    this.products().reduce((sum, p) => sum + p.price * p.quantity, 0)
  );

  readonly withImages = computed(() =>
    this.products().filter(p => p.imageUrls?.length > 0).length
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
      error: (err) => {
        this.loading.set(false);
        this.toast.error(err.error || 'Unable to load your products.');
      },
    });
  }

  deleteProduct(id: string): void {
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
          error: (err) => {
            this.deletingId.set(null);
            this.toast.error(err.error || 'Unable to delete product.');
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
    this.selectedFiles.set([]);
    this.imagePreviewUrls.set([]);
    this.fileError.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const newFiles = Array.from(input.files);
    input.value = '';

    const remaining = 3 - this.selectedFiles().length;
    if (remaining <= 0) {
      this.fileError.set('Maximum 3 images per product.');
      return;
    }

    const toAdd = newFiles.slice(0, remaining);
    for (const file of toAdd) {
      const error = this.mediaService.validateImage(file);
      if (error) {
        this.fileError.set(error);
        return;
      }
    }

    this.fileError.set(null);
    this.selectedFiles.update(list => [...list, ...toAdd]);
    this.imagePreviewUrls.update(list => [...list, ...toAdd.map(f => URL.createObjectURL(f))]);
  }

  removeSelectedImage(index: number): void {
    this.selectedFiles.update(list => list.filter((_, i) => i !== index));
    this.imagePreviewUrls.update(list => list.filter((_, i) => i !== index));
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
          const files = this.selectedFiles();
          const localPreviews = this.imagePreviewUrls();

          if (!files.length) {
            this.products.update((list) => [product, ...list]);
            this.submitting.set(false);
            this.closeAddModal();
            return;
          }

          // Optimistic update avec previews locaux
          this.products.update((list) => [{ ...product, imageUrls: localPreviews }, ...list]);

          this.mediaService.uploadImage(product.userId, product.id, files, 'Product').subscribe({
            next: () => {
              this.submitting.set(false);
              this.closeAddModal();
            },
            error: (err) => {
              this.submitting.set(false);
              this.toast.error(err.error || 'Product created, but images failed to upload.');
              this.closeAddModal();
            },
          });
        },
        error: (err) => {
          console.log(err);

          this.submitting.set(false);
          this.toast.error(
            err.error || 'Unable to create product.'
          );
        },
      });
  }

  // --- Edit product modal ---

  openEditModal(product: ProductDto): void {
    this.editingProduct.set(product);
    this.editProductForm.setValue({
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
    });
    this.existingImageUrls.set(product.imageUrls ?? []);
    this.selectedEditFiles.set([]);
    this.selectedEditFilePreviews.set([]);
    this.editFileError.set(null);
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingProduct.set(null);
    this.editProductForm.reset();
    this.existingImageUrls.set([]);
    this.selectedEditFiles.set([]);
    this.selectedEditFilePreviews.set([]);
    this.editFileError.set(null);
  }

  onEditFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const newFiles = Array.from(input.files);
    input.value = '';

    const remaining = 3 - this.existingImageUrls().length - this.selectedEditFiles().length;
    if (remaining <= 0) {
      this.editFileError.set('Maximum 3 images per product.');
      return;
    }

    const toAdd = newFiles.slice(0, remaining);
    for (const file of toAdd) {
      const error = this.mediaService.validateImage(file);
      if (error) {
        this.editFileError.set(error);
        return;
      }
    }

    this.editFileError.set(null);
    this.selectedEditFiles.update(list => [...list, ...toAdd]);
    this.selectedEditFilePreviews.update(list => [...list, ...toAdd.map(f => URL.createObjectURL(f))]);
  }

  removeExistingImage(index: number): void {
    this.existingImageUrls.update(list => list.filter((_, i) => i !== index));
  }

  removeEditFile(index: number): void {
    this.selectedEditFiles.update(list => list.filter((_, i) => i !== index));
    this.selectedEditFilePreviews.update(list => list.filter((_, i) => i !== index));
  }

  submitEditProduct(): void {
    if (this.editProductForm.invalid) {
      this.editProductForm.markAllAsTouched();
      return;
    }

    const current = this.editingProduct();
    if (!current) return;

    this.submittingEdit.set(true);
    const { name, description, price, quantity } = this.editProductForm.getRawValue();

    this.productService
      .update(current.id, {
        name: name!,
        description: description!,
        price: price!,
        quantity: quantity!,
      })
      .subscribe({
        next: (updated) => {
          const files = this.selectedEditFiles();
          const existingUrls = this.existingImageUrls();
          const localPreviews = this.selectedEditFilePreviews();

          if (!files.length) {
            this.products.update((list) =>
              list.map((p) => (p.id === current.id ? { ...p, ...updated, imageUrls: existingUrls } : p))
            );
            this.submittingEdit.set(false);
            this.closeEditModal();
            return;
          }

          // Optimistic update
          const optimisticUrls = [...existingUrls, ...localPreviews];
          this.products.update((list) =>
            list.map((p) => (p.id === current.id ? { ...p, ...updated, imageUrls: optimisticUrls } : p))
          );

          this.mediaService.uploadImage(current.userId, current.id, files, 'Product').subscribe({
            next: () => {
              this.submittingEdit.set(false);
              this.closeEditModal();
            },
            error: (err) => {
              this.products.update((list) =>
                list.map((p) => (p.id === current.id ? { ...p, ...updated, imageUrls: existingUrls } : p))
              );
              this.submittingEdit.set(false);
              this.toast.error(err.error || 'Product updated, but images failed to upload.');
              this.closeEditModal();
            },
          });
        },
        error: (err) => {
          this.submittingEdit.set(false);
          this.toast.error(err.error || 'Unable to update product.');
        },
      });
  }
}
