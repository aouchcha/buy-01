import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

import { ProfileService } from '../../../../core/services/profile';
import { User, Role } from '../../../../core/models/user';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly http = inject(HttpClient);

  readonly Role = Role;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly profile = signal<User | null>(null);

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
      },
    });
  }

  updateFirstName(firstName: string): void {
    this.profile.update((user) =>
      user
        ? {
          ...user,
          firstName,
        }
        : null
    );
  }

  updateLastName(lastName: string): void {
    this.profile.update((user) =>
      user
        ? {
          ...user,
          lastName,
        }
        : null
    );
  }

  updateEmail(email: string): void {
    this.profile.update((user) =>
      user
        ? {
          ...user,
          email,
        }
        : null
    );
  }

  updateRole(role: Role): void {
    this.profile.update((user) =>
      user
        ? {
          ...user,
          role,
        }
        : null
    );
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
      },
      error: (err) => {
        console.error(err);
        this.error.set('Unable to save profile.');
        this.saving.set(false);
      },
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files?.length) {
      return;
    }

    const file = input.files[0];

    const formData = new FormData();
    formData.append('file', file);

    this.http
      .post<{ url: string }>(
        'http://localhost:8080/api/media/upload',
        formData
      )
      .subscribe({
        next: (response) => {
          this.profile.update((user) =>
            user
              ? {
                ...user,
                profilePictureUrl: response.url,
              }
              : null
          );
        },
        error: (err) => {
          console.error(err);
        },
      });
  }
}