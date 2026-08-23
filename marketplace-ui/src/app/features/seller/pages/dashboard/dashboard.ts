import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../../core/services/auth';
import { Product } from '../../../../core/services/product';
import { CATEGORY_LABELS, CATEGORY_OPTIONS, Category, ProductDto } from '../../../../core/models/product';
import { Media } from '../../../../core/services/media';
import { Navbar } from '../../../../layout/navbar/navbar';
import { ConfirmService } from '../../../../core/services/confirm';
import { ToastService } from '../../../../core/services/toast.service';
import { MatIconModule } from '@angular/material/icon';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    Navbar,
    MatIconModule,
    BaseChartDirective
  ],
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

  readonly user = this.auth.getCurrentUser();
  readonly seller = this.user;

  // --- Role checks ---
  readonly isSeller = computed(() => this.auth.isSeller());
  readonly isClient = computed(() => !this.isSeller());

  // --- Product State ---
  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal<boolean>(true);
  readonly deletingId = signal<string | null>(null);
  readonly imageIndexes = signal<Record<string, number>>({});

  categoryLabel(category: Category): string {
    return CATEGORY_LABELS[category];
  }

  getImageIndex(productId: string): number {
    return this.imageIndexes()[productId] ?? 0;
  }

  nextImage(product: ProductDto, event: Event): void {
    event.stopPropagation();
    const current = this.getImageIndex(product.id);
    this.imageIndexes.update(m => ({ ...m, [product.id]: (current + 1) % product.imageUrls.length }));
  }

  prevImage(product: ProductDto, event: Event): void {
    event.stopPropagation();
    const current = this.getImageIndex(product.id);
    this.imageIndexes.update(m => ({ ...m, [product.id]: (current - 1 + product.imageUrls.length) % product.imageUrls.length }));
  }

  // --- Seller Dashboard Metrics & Lists ---
  readonly totalRevenue = computed(() =>
    this.products().reduce((sum, p) => sum + (p.price * (p.quantity || 0)), 0)
  );

  readonly totalUnitsSold = computed(() =>
    this.products().reduce((sum, p) => sum + (p.quantity || 0), 0)
  );

  readonly bestSellingProducts = computed(() =>
    [...this.products()].sort((a, b) => (b.quantity || 0) - (a.quantity || 0))
  );

  readonly totalValue = computed(() =>
    this.products().reduce((sum, p) => sum + p.price * p.quantity, 0)
  );

  readonly withImages = computed(() =>
    this.products().filter(p => p.imageUrls?.length > 0).length
  );

  // --- Client Dashboard Metrics & Lists ---
  readonly totalSpent = signal<number>(0);
  readonly orderCount = signal<number>(0);
  readonly totalItemsBought = signal<number>(0);
  readonly mostBoughtProducts = signal<ProductDto[]>([]);

  // --- Chart Configurations & Data ---
  readonly chartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: { font: { family: 'Inter' } }
      }
    }
  };

  readonly sellerUnitsChartData = computed<ChartData<'pie'>>(() => ({
    labels: this.products().map(p => p.name),
    datasets: [{
      data: this.products().map(p => p.quantity || 0),
      backgroundColor: ['#a08060', '#7a5f45', '#c9b99a', '#e8dcc4', '#4a4038']
    }]
  }));

  readonly sellerRevenueChartData = computed<ChartData<'line'>>(() => ({
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    datasets: [{
      label: 'Revenue (MAD)',
      data: [1200, 1900, 3000, 5000, 2000, this.totalRevenue()],
      borderColor: '#7a5f45',
      backgroundColor: 'rgba(122, 95, 69, 0.1)',
      fill: true,
      tension: 0.4
    }]
  }));

  readonly clientCategoryChartData = computed<ChartData<'doughnut'>>(() => ({
    labels: ['Electronics', 'Home Decor', 'Clothing'],
    datasets: [{
      data: [300, 450, 200],
      backgroundColor: ['#a08060', '#7a5f45', '#c9b99a']
    }]
  }));

  readonly clientSpendingChartData = computed<ChartData<'bar'>>(() => ({
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    datasets: [{
      label: 'Spent (MAD)',
      data: [400, 600, 800, 200, 900, this.totalSpent()],
      backgroundColor: '#a08060'
    }]
  }));

  // --- Add Product Modal State ---
  readonly showAddModal = signal(false);
  readonly submitting = signal(false);
  readonly selectedFiles = signal<File[]>([]);
  readonly imagePreviewUrls = signal<string[]>([]);
  readonly fileError = signal<string | null>(null);

  readonly categoryOptions = CATEGORY_OPTIONS;

  readonly addProductForm = this.fb.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
    category: [null as Category | null, Validators.required],
  });

  // --- Edit Product Modal State ---
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
    category: [null as Category | null, Validators.required],
  });

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

  // --- Add Product Modal ---

  openAddModal(): void {
    this.showAddModal.set(true);
  }

  onAddBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.closeAddModal();
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
    this.confirmService.open({
      title: 'Remove this image?',
      message: 'This image will be removed from the upload list.',
      confirmText: 'Remove',
      cancelText: 'Cancel',
      danger: true,
    }).subscribe(confirmed => {
      if (!confirmed) return;
      this.selectedFiles.update(list => list.filter((_, i) => i !== index));
      this.imagePreviewUrls.update(list => list.filter((_, i) => i !== index));
    });
  }

  submitAddProduct(): void {
    if (this.addProductForm.invalid) {
      this.addProductForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { name, description, price, quantity, category } = this.addProductForm.getRawValue();

    this.productService
      .create({
        name: name!,
        description: description!,
        price: price!,
        quantity: quantity!,
        category: category!,
      })
      .subscribe({
        next: (product) => {
          this.toast.success("Product upload with success");
          const files = this.selectedFiles();
          const localPreviews = this.imagePreviewUrls();

          if (!files.length) {
            this.products.update((list) => [product, ...list]);
            this.submitting.set(false);
            this.closeAddModal();
            return;
          }

          this.products.update((list) => [{ ...product, imageUrls: localPreviews }, ...list]);

          this.mediaService.uploadImage(product.userId, product.id, files, 'Product').then((observable) => {
            observable.subscribe({
              next: (images) => {
                this.products.update((list) =>
                  list.map((p) =>
                    p.id === product.id ? { ...p, imageUrls: images.map((img) => img.url) } : p
                  )
                );
                this.submitting.set(false);
                this.toast.success("Picture upload with success");
                this.closeAddModal();
              },
              error: (err) => {
                this.submitting.set(false);
                this.toast.error(err.error || 'Product created, but images failed to upload.');
                this.closeAddModal();
              },
            });
          });
        },
        error: (err) => {
          this.submitting.set(false);
          this.toast.error(err.error || 'Unable to create product.');
        },
      });
  }

  // --- Edit Product Modal ---

  onEditBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.closeEditModal();
  }

  openEditModal(product: ProductDto): void {
    this.editingProduct.set(product);
    this.editProductForm.setValue({
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
      category: product.category,
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
    this.confirmService.open({
      title: 'Remove this image?',
      message: 'This image will be deleted from the product.',
      confirmText: 'Remove',
      cancelText: 'Cancel',
      danger: true,
    }).subscribe(confirmed => {
      if (!confirmed) return;
      this.existingImageUrls.update(list => list.filter((_, i) => i !== index));
    });
  }

  removeEditFile(index: number): void {
    this.confirmService.open({
      title: 'Remove this image?',
      message: 'This image will be removed from the upload list.',
      confirmText: 'Remove',
      cancelText: 'Cancel',
      danger: true,
    }).subscribe(confirmed => {
      if (!confirmed) return;
      this.selectedEditFiles.update(list => list.filter((_, i) => i !== index));
      this.selectedEditFilePreviews.update(list => list.filter((_, i) => i !== index));
    });
  }

  submitEditProduct(): void {
    if (this.editProductForm.invalid) {
      this.editProductForm.markAllAsTouched();
      return;
    }

    const current = this.editingProduct();
    if (!current) return;

    this.submittingEdit.set(true);
    const { name, description, price, quantity, category } = this.editProductForm.getRawValue();

    this.productService
      .update(current.id, {
        name: name!,
        description: description!,
        price: price!,
        quantity: quantity!,
        category: category!,
      })
      .subscribe({
        next: (updated) => {
          this.toast.success("Product update with success");
          const files = this.selectedEditFiles();
          const existingUrls = this.existingImageUrls();
          const deletedUrls = current.imageUrls.filter(
            url => !existingUrls.includes(url)
          );
          const localPreviews = this.selectedEditFilePreviews();

          if (!files.length) {
            this.products.update((list) =>
              list.map((p) => (p.id === current.id ? { ...p, ...updated, imageUrls: existingUrls } : p))
            );
          }

          const optimisticUrls = [...existingUrls, ...localPreviews];
          this.products.update((list) =>
            list.map((p) => (p.id === current.id ? { ...p, ...updated, imageUrls: optimisticUrls } : p))
          );

          if (!files.length && !deletedUrls.length) {
            this.submittingEdit.set(false);
            return this.closeEditModal();
          }

          this.mediaService.updateImages(current.userId, current.id, deletedUrls, files, 'Product').subscribe({
            next: (images) => {
              this.products.update(list =>
                list.map(p =>
                  p.id === current.id
                    ? { ...p, imageUrls: images.map(img => img.url) }
                    : p
                )
              );

              this.toast.success("Picture update with success");
              this.submittingEdit.set(false);
              this.closeEditModal();
            },
            error: (err) => {
              this.products.update(list =>
                list.map(p =>
                  p.id === current.id
                    ? { ...p, ...updated, imageUrls: existingUrls }
                    : p
                )
              );

              this.submittingEdit.set(false);
              this.toast.error(
                err.error || 'Product updated, but images failed to upload.'
              );
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