import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Auth } from '../../../../core/services/auth';
import { Product } from '../../../../core/services/product';
import { Media } from '../../../../core/services/media';
import { ProductDto } from '../../../../core/models/product';
import { MediaResponse } from '../../../../core/models/media';
import { Navbar } from '../../../../layout/navbar/navbar';
import { ConfirmService } from '../../../../core/services/confirm';
import { ToastService } from '../../../../core/services/toast.service';
import { MatIconModule } from '@angular/material/icon';

interface MediaItem {
  id: string;
  url: string;
  productId: string;
  productName: string;
}

@Component({
  selector: 'app-media-management',
  standalone: true,
  imports: [CommonModule, Navbar, MatIconModule],
  templateUrl: './media-management.html',
  styleUrl: './media-management.scss',
})
export class MediaManagement implements OnInit {
  private readonly auth = inject(Auth);
  private readonly productService = inject(Product);
  private readonly mediaService = inject(Media);
  private readonly confirmService = inject(ConfirmService);
  private readonly toast = inject(ToastService);

  readonly seller = this.auth.getCurrentUser();

  readonly products = signal<ProductDto[]>([]);
  readonly medias = signal<MediaResponse[]>([]);
  readonly loading = signal(true);
  readonly deletingUrl = signal<string | null>(null);

  // Upload state
  readonly showUploadModal = signal(false);
  readonly uploading = signal(false);
  readonly selectedFiles = signal<File[]>([]);
  readonly previewUrls = signal<string[]>([]);
  readonly fileError = signal<string | null>(null);
  readonly selectedProductId = signal<string | null>(null);

  readonly allMedia = computed<MediaItem[]>(() => {
    const productMap = new Map(this.products().map((p) => [p.id, p.name]));
    return this.medias().map((m) => ({
      id: m.id,
      url: m.url,
      productId: m.productId ?? '',
      productName: productMap.get(m.productId ?? '') ?? 'Unknown product',
    }));
  });

  readonly totalImages = computed(() => this.medias().length);
  readonly totalProducts = computed(() => this.products().length);
  readonly productsWithImages = computed(
    () => new Set(this.medias().map((m) => m.productId).filter(Boolean)).size
  );

  ngOnInit(): void {
    this.loadProducts();
    this.loadMedias();
  }

  private loadMedias(): void {
    this.mediaService.getMyImages().subscribe({
      next: (medias) => this.medias.set(medias),
      error: (err) => {
        this.toast.error(err.error || 'Unable to load media.');
      },
    });
  }

  private loadProducts(): void {
    this.loading.set(true);
    this.productService.getMyProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(err.error || 'Unable to load media.');
      },
    });
  }

  deleteImage(item: MediaItem): void {
    this.confirmService
      .open({
        title: 'Delete this image?',
        message: `This image from "${item.productName}" will be permanently removed.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        danger: true,
      })
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.deletingUrl.set(item.url);

        this.mediaService.deleteImage(item.id).subscribe({
          next: () => {
            this.medias.update((list) =>
              list.filter((m) => m.id !== item.id)
            );
            this.deletingUrl.set(null);
            this.toast.success('Image deleted.');
          },
          error: (err) => {
            console.log(err);

            this.deletingUrl.set(null);
            this.toast.error(err.error || 'Unable to delete image.');
          },
        });
      });
  }

  // Upload modal

  openUploadModal(): void {
    this.selectedProductId.set(this.products()[0]?.id ?? null);
    this.showUploadModal.set(true);
  }

  closeUploadModal(): void {
    this.showUploadModal.set(false);
    this.selectedFiles.set([]);
    this.previewUrls.set([]);
    this.fileError.set(null);
    this.selectedProductId.set(null);
  }

  onProductChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedProductId.set(select.value || null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const newFiles = Array.from(input.files);
    input.value = '';

    const remaining = 3 - this.selectedFiles().length;
    if (remaining <= 0) {
      this.fileError.set('Maximum 3 images at a time.');
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
    this.selectedFiles.update((list) => [...list, ...toAdd]);
    this.previewUrls.update((list) => [
      ...list,
      ...toAdd.map((f) => URL.createObjectURL(f)),
    ]);
  }

  removeSelectedFile(index: number): void {
    this.selectedFiles.update((list) => list.filter((_, i) => i !== index));
    this.previewUrls.update((list) => list.filter((_, i) => i !== index));
  }

  submitUpload(): void {
    const productId = this.selectedProductId();
    const files = this.selectedFiles();

    if (!productId) {
      this.fileError.set('Select a product first.');
      return;
    }
    if (!files.length) {
      this.fileError.set('Select at least one image.');
      return;
    }

    const seller = this.seller;
    if (!seller) return;

    this.uploading.set(true);
    this.mediaService.updateImages(seller.id, productId, [], files, 'Product').subscribe({
      next: (res) => {
        // this.medias.update((list) =>
        //   [...list, ...res]
        // );
        this.medias.update((list) => {
          const existingIds = new Set(list.map((m) => m.id));
          const newItems = res.filter((m) => !existingIds.has(m.id));
          return [...list, ...newItems];
        });

        this.uploading.set(false);
        this.closeUploadModal();
        this.toast.success('Images uploaded.');
      },
      error: (err) => {
        this.uploading.set(false);
        this.toast.error(err.error || 'Upload failed.');
      },
    });
  }
}
