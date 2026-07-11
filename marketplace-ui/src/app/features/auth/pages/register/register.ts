import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Auth } from '../../../../core/services/auth';

type Role = 'CLIENT' | 'SELLER';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly authService = inject(Auth);
  private readonly router = inject(Router);

  firstName = '';
  lastName = '';
  email = '';
  password = '';
  confirmPassword = '';

  readonly role = signal<Role>('CLIENT');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly profilePicture = signal<File | null>(null);
  readonly profilePreview = signal<string | null>(null);

  selectRole(role: Role): void {
    this.role.set(role);
  }

  togglePassword(): void {
    this.showPassword.update((value) => !value);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.error.set('Profile picture must be an image file.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.error.set('Profile picture must be smaller than 5MB.');
      return;
    }

    this.error.set(null);
    this.profilePicture.set(file);

    const reader = new FileReader();
    reader.onload = () => this.profilePreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  removeProfilePicture(): void {
    this.profilePicture.set(null);
    this.profilePreview.set(null);
  }

  onSubmit(): void {
    this.error.set(null);

    if (this.password !== this.confirmPassword) {
      this.error.set('Passwords do not match.');
      return;
    }

    this.loading.set(true);

    const formData = new FormData();
    formData.append('email', this.email);
    formData.append('password', this.password);
    formData.append('firstName', this.firstName);
    formData.append('lastName', this.lastName);
    formData.append('role', this.role());

    const picture = this.profilePicture();
    if (picture) {
      formData.append('profilePicture', picture);
    }

    this.authService.register(formData).subscribe({
      next: (response) => {
        localStorage.setItem('token', response.token);
        this.loading.set(false);
        this.router.navigate(['/']);
      },
      error: (err) => {
        console.error(err);
        this.error.set(
          err?.error ?? 'Could not create your account. Please try again.',
        );
        this.loading.set(false);
      },
    });
  }
}