import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Auth } from '../../../../core/services/auth';
import { Role } from '../../../../core/models/user';
import { Media } from '../../../../core/services/media';
import { ToastService } from '../../../../core/services/toast.service';
import { registerRequest } from '../../../../core/models/auth-request';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword
    ? { passwordsMismatch: true }
    : null;
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly authService = inject(Auth);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly mediaService = inject(Media);
  private readonly toast = inject(ToastService);

  readonly Role = Role;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly profilePicture = signal<File | null>(null);
  readonly profilePreview = signal<string | null>(null);

  readonly registerForm = this.fb.group(
    {
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
      role: [Role.CLIENT, Validators.required],
    },
    { validators: passwordsMatchValidator },
  );

  selectRole(role: Role): void {
    this.registerForm.controls.role.setValue(role);
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

    if (file.size > 2 * 1024 * 1024) {
      this.error.set('Profile picture must be smaller than 2MB.');
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

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      if (this.registerForm.errors?.['passwordsMismatch']) {
        this.error.set('Passwords do not match.');
      }
      return;
    }

    this.loading.set(true);

    const { firstName, lastName, email, password, role } = this.registerForm.getRawValue();

    // const formData = new FormData();
    // formData.append('email', email!);
    // formData.append('password', password!);
    // formData.append('firstName', firstName!);
    // formData.append('lastName', lastName!);
    // formData.append('role', role!);
    const request: registerRequest = {
      email: email!,
      password: password!,
      firstName: firstName!,
      lastName: lastName!,
      role: role!,
    };

    // const picture = this.profilePicture();
    // if (picture) {
    //   formData.append('profilePicture', picture);
    // }

    this.authService.register(request).subscribe({
      next: (response) => {

        const picture = this.profilePicture();
        if (picture) {
          this.mediaService.uploadImage(response.user.id, null, [picture], 'Avatar').subscribe({
            next: () => {

            },
            error: (err) => {
              this.toast.error(
                err.error || 'image upload failed.'
              );
              // this.loading.set(false);
            },
          });
        }

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
