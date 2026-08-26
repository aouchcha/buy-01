import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ProfileService } from '../../../../core/services/profile';
import { Media } from '../../../../core/services/media';
import { Auth } from '../../../../core/services/auth';
import { User, Role } from '../../../../core/models/user';
import { Navbar } from '../../../../layout/navbar/navbar';
import { ToastService } from '../../../../core/services/toast.service';
import { Product } from '../../../../core/services/product';
import { ProductDto } from '../../../../core/models/product';
import { RouterLink } from '@angular/router';


@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, Navbar, RouterLink],
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
  readonly saveError = signal<string | null>(null);
  readonly isEditing = signal(false);

  readonly profile = signal<User | null>(null);

  private snapshot: User | null = null;

  readonly selectedEditFiles = signal<File | null>(null);
  readonly avatarPreviewUrl = signal<string | null>(null);
  readonly imageIndexes = signal<Record<string, number>>({});

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


  readonly defaultAvatar =
    'data:image/svg+xml;utf8,' +
    encodeURIComponent(
      `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
         <rect width="100" height="100" fill="#e8dcc4"/>
         <circle cx="50" cy="38" r="18" fill="#a08060"/>
         <path d="M20 88c0-19 13-34 30-34s30 15 30 34" fill="#a08060"/>
       </svg>`
    );


  ngOnInit(): void {
    this.loadProfile();
    this.fetchProducts();
  }


  onAvatarError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = this.defaultAvatar;
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
      }
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

    // remove selected image if user cancels
    this.selectedEditFiles.set(null);

    this.isEditing.set(false);
  }


  updateFirstName(firstName: string): void {
    this.profile.update(user =>
      user
        ? {
            ...user,
            firstName
          }
        : null
    );
  }


  updateLastName(lastName: string): void {
    this.profile.update(user =>
      user
        ? {
            ...user,
            lastName
          }
        : null
    );
  }



  saveProfile(): void {

    const user = this.profile();
    const original = this.snapshot;
    const selectedFile = this.selectedEditFiles();


    if (!user) {
      return;
    }

    this.saveError.set(null);

    const firstName = user.firstName?.trim() ?? '';
    const lastName = user.lastName?.trim() ?? '';

    if (!firstName) {
      this.saveError.set('First name is required.');
      return;
    }
    if (!lastName) {
      this.saveError.set('Last name is required.');
      return;
    }
    if (firstName.length < 3) {
      this.saveError.set('First name must be at least 3 characters.');
      return;
    }
    if (lastName.length < 2) {
      this.saveError.set('Last name must be at least 2 characters.');
      return;
    }

    const nameChanged =
      !original ||
      user.firstName !== original.firstName ||
      user.lastName !== original.lastName;


    const avatarChanged = !!selectedFile;



    // Nothing changed
    if (!nameChanged && !avatarChanged) {
      this.isEditing.set(false);
      return;
    }


    this.saving.set(true);

    if (nameChanged) {

      this.profileService.updateMe(user).subscribe({

        next: (updatedUser) => {

          this.profile.set(updatedUser);
          this.auth.updateCurrentUser(updatedUser);



          if (avatarChanged) {

            this.uploadAvatar();

          } else {

            this.finishSaving();
            this.toast.success(
              'Profile updated successfully.'
            );

          }

        },


        error: (err) => {

          console.error(err);

          this.saveError.set(
            err?.error?.message || err?.error || 'Unable to save profile.'
          );

          this.saving.set(false);

          this.toast.error(
            err?.error?.message || err?.error || 'Unable to save profile. Please try again.'
          );

        }

      });


    } else {

      // Only avatar changed
      this.uploadAvatar();

    }

  }





  private uploadAvatar(): void {

    const file = this.selectedEditFiles();
    const user = this.profile();



    if (!file || !user) {

      this.finishSaving();

      return;

    }



    const deletedUrls =
      user.profilePictureUrl
        ? [user.profilePictureUrl]
        : [];




    this.mediaService.updateImages(
      user.id,
      null,
      deletedUrls,
      [file],
      'Avatar'

    ).subscribe({

      next: (media) => {


        if (
          media.length > 0 &&
          media[0].url
        ) {

          const updatedUser: User = {

            ...user,

            profilePictureUrl:
              media[0].url

          };


          this.profile.set(updatedUser);

          this.auth.updateCurrentUser(
            updatedUser
          );

        }



        // clear selected file
        this.selectedEditFiles.set(null);



        this.finishSaving();



        this.toast.success(
          'Profile updated successfully.'
        );

      },


      error: (err) => {

        console.error(err);

        this.saving.set(false);

        this.toast.error(
          err.error ||
          'Unable to upload photo.'
        );

      }

    });

  }





  private finishSaving(): void {

    this.saving.set(false);
    this.saveError.set(null);
    this.avatarPreviewUrl.set(null);

    this.isEditing.set(false);

    this.snapshot = this.profile();

  }





  onAvatarSelected(event: Event): void {

    const input =
      event.target as HTMLInputElement;



    if (!input.files?.length) {
      return;
    }



    const file = input.files[0];



    const validationError =
      this.mediaService.validateImage(file);



    if (validationError) {

      this.toast.error(
        validationError
      );

      input.value = '';

      return;

    }



    this.selectedEditFiles.set(file);
    this.avatarPreviewUrl.set(URL.createObjectURL(file));

    input.value = '';

  }





  private fetchProducts(): void {

    this.productsLoading.set(true);



    this.productService.getMyProducts()
      .subscribe({

        next: (products) => {

          this.products.set(products);

          this.productsLoading.set(false);

        },


        error: (err) => {

          this.productsLoading.set(false);

          this.toast.error(
            err.error ||
            'Unable to load your products.'
          );

        }

      });

  }


  public sellerEarnings(): number {

    return 0;

  }

  public bestSellingProducts(): ProductDto[] {

    return [];

  }

  public totalMoneySpent(): number {

    return 0;

  }

  public mostBoughtProducts(): ProductDto[] {

    return [];

  }

  public bestProducts(): ProductDto[] {

    return [];

  }

}