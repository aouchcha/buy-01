import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ProfileService } from '../../../../core/services/profile';
import { Media } from '../../../../core/services/media';
import { Auth } from '../../../../core/services/auth';
import { User, Role } from '../../../../core/models/user';
import { Navbar } from '../../../../layout/navbar/navbar';
import { ToastService } from '../../../../core/services/toast.service';
import { Product } from '../../../../core/services/product'; // adjust path if needed
import { ProductDto } from '../../../../core/models/product';


@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, Navbar],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly mediaService = inject(Media);
  private readonly auth = inject(Auth);
  private readonly toast = inject(ToastService);
  private readonly productService = inject(Product);


  readonly products = signal<ProductDto[]>([]);
  readonly productsLoading = signal(true);

  readonly Role = Role;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly isEditing = signal(false);

  readonly profile = signal<User | null>(null);

  private snapshot: User | null = null;

  readonly defaultAvatar =
    'data:image/svg+xml;utf8,' +
    encodeURIComponent(
      `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
         <rect width="100" height="100" fill="#e8dcc4"/>
         <circle cx="50" cy="38" r="18" fill="#a08060"/>
         <path d="M20 88c0-19 13-34 30-34s30 15 30 34" fill="#a08060"/>
       </svg>`
    );

  onAvatarError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = this.defaultAvatar;
  }

  ngOnInit(): void {
    this.loadProfile();
    this.fetchProducts();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.error.set(null);

    this.profileService.getMe().subscribe({
      next: (user) => {
        console.log(user);

        this.profile.set(user);
        this.auth.updateCurrentUser(user);
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.error.set('Unable to load profile.');
        this.loading.set(false);
        this.toast.error('Unable to load profile.');
      },
    });
  }

  startEditing(): void {
    this.snapshot = this.profile();
    this.isEditing.set(true);
  }

  cancelEditing(): void {
    if (this.snapshot) {
      this.profile.set(this.snapshot);
    }
    this.isEditing.set(false);
  }

  updateFirstName(firstName: string): void {
    this.profile.update((user) => (user ? { ...user, firstName } : null));
  }

  updateLastName(lastName: string): void {
    this.profile.update((user) => (user ? { ...user, lastName } : null));
  }

  saveProfile(): void {
    const user = this.profile();
    const original = this.snapshot;

    if (!user) {
      return;
    }

    const nameChanged =
      !original ||
      user.firstName !== original.firstName ||
      user.lastName !== original.lastName;

    if (!nameChanged) {
      // Nothing to persist — just exit edit mode.
      this.isEditing.set(false);
      return;
    }

    this.saving.set(true);

    this.profileService.updateMe(user).subscribe({
      next: (updatedUser) => {
        console.log(updatedUser);

        this.profile.set(updatedUser);
        this.auth.updateCurrentUser(updatedUser);
        this.saving.set(false);
        this.isEditing.set(false);
        this.toast.success('Profile updated successfully.');
      },
      error: (err) => {
        console.error(err);
        this.error.set('Unable to save profile.');
        this.saving.set(false);
        this.toast.error('Unable to save profile. Please try again.');
      },
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files?.length) {
      return;
    }

    const file = input.files[0];

    const validationError = this.mediaService.validateImage(file);
    if (validationError) {
      this.toast.error(validationError);
      input.value = '';
      return;
    }

    const profilePictureUrl = this.profile()?.profilePictureUrl;

    const deletedUrls: string[] = profilePictureUrl
      ? [profilePictureUrl]
      : [];

    const currentUser = this.profile();
    if (!currentUser) return;

    this.mediaService.updateImages(
      currentUser.id,
      null,
      deletedUrls,
      [file],
      'Avatar'
    ).subscribe({
      next: (media) => {
        this.toast.success('Photo updated successfully.');

        if (media.length > 0 && media[0].url) {
          this.profile.update(user =>
            user
              ? {
                ...user,
                profilePictureUrl: media[0].url,
              }
              : null
          );
          const updated = this.profile();
          if (updated) this.auth.updateCurrentUser(updated);
        }
      },
      error: (err) => {
        console.error(err);
        this.toast.error(err.error || 'Unable to upload photo.');
      },
    });
    input.value = '';
  }

  private fetchProducts(): void {
    this.productsLoading.set(true);
    this.productService.getMyProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.productsLoading.set(false);
      },
      error: (err) => {
        this.productsLoading.set(false);
        this.toast.error(err.error || 'Unable to load your products.');
      },
    });
  }
}