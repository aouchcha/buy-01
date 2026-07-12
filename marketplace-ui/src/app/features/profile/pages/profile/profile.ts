import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

import { ProfileService } from '../../../../core/services/profile';
import { User, Role } from '../../../../core/models/user';
import { Navbar } from '../../../../layout/navbar/navbar';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, Navbar],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

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
  }

  loadProfile(): void {
    this.loading.set(true);
    this.error.set(null);

    this.profileService.getMe().subscribe({
      next: (user) => {
        this.profile.set(user);
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

  updateEmail(email: string): void {
    this.profile.update((user) => (user ? { ...user, email } : null));
  }

  updateRole(role: Role): void {
    this.profile.update((user) => (user ? { ...user, role } : null));
  }

  saveProfile(): void {
    const user = this.profile();

    if (!user) {
      return;
    }

    this.saving.set(true);

    this.profileService.updateMe(user).subscribe({
      next: (updatedUser) => {
        this.profile.set(updatedUser);
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

    if (!file.type.startsWith('image/')) {
      this.toast.error('Only image files are allowed.');
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.toast.error('Image must be 2MB or smaller.');
      input.value = '';
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.http
      .post<{ url: string }>('http://localhost:8080/api/media/upload', formData)
      .subscribe({
        next: (response) => {
          this.profile.update((user) =>
            user ? { ...user, profilePictureUrl: response.url } : null
          );
          this.toast.success('Photo updated.');
        },
        error: (err) => {
          console.error(err);
          this.toast.error('Unable to upload photo.');
        },
      });
  }
}