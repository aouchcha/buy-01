import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  imports: [CommonModule, ReactiveFormsModule, Navbar, MatIconModule],
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
  readonly selectedFile = signal<File | null>(null);
  readonly imagePreviewUrl = signal<string | null>(null);
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
  readonly selectedEditFile = signal<File | null>(null);
  readonly editImagePreviewUrl = signal<string | null>(null);
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
        this.products.set(products.map(p => ({
          ...p,
          imageUrl: p.imageUrl ?? p.imageUrls?.[0],
        })));
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
            error: (err) => {
              // Product was created but the image upload failed — keep it visible without an image.
              this.products.update((list) => [product, ...list]);
              this.submitting.set(false);
              this.toast.error(
                err.error || 'image upload failed.'
              );
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
    this.editImagePreviewUrl.set(product.imageUrl ?? null);
    this.selectedEditFile.set(null);
    this.editFileError.set(null);
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingProduct.set(null);
    this.editProductForm.reset();
    this.selectedEditFile.set(null);
    this.editImagePreviewUrl.set(null);
    this.editFileError.set(null);
  }

  onEditFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.selectedEditFile.set(null);
      this.editFileError.set(null);
      return;
    }

    const error = this.mediaService.validateImage(file);
    if (error) {
      this.editFileError.set(error);
      this.selectedEditFile.set(null);
      input.value = '';
      return;
    }

    this.editFileError.set(null);
    this.selectedEditFile.set(file);
    this.editImagePreviewUrl.set(URL.createObjectURL(file));
  }

  removeEditSelectedImage(): void {
    this.selectedEditFile.set(null);
    this.editImagePreviewUrl.set(null);
    this.editFileError.set(null);
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
          const file = this.selectedEditFile();
          if (!file) {
            this.products.update((list) =>
              list.map((p) => (p.id === current.id ? { ...p, ...updated } : p))
            );
            this.submittingEdit.set(false);
            this.closeEditModal();
            return;
          }

          this.mediaService.uploadImage(current.id, file).subscribe({
            next: (media) => {
              this.products.update((list) =>
                list.map((p) =>
                  p.id === current.id ? { ...p, ...updated, imageUrl: media.url } : p
                )
              );
              this.submittingEdit.set(false);
              this.closeEditModal();
            },
            error: (err) => {
              this.products.update((list) =>
                list.map((p) => (p.id === current.id ? { ...p, ...updated } : p))
              );
              this.submittingEdit.set(false);
              this.toast.error(err.error || 'Product updated, but the image failed to upload.');
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