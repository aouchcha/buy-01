import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Auth } from '../../../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly authService = inject(Auth);
  private readonly router = inject(Router);

  email = '';
  password = '';

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  togglePassword(): void {
    this.showPassword.update((value) => !value);
  }

  onSubmit(): void {
    this.error.set(null);
    this.loading.set(true);

    this.authService
      .login({
        email: this.email,
        password: this.password,
      })
      .subscribe({
        next: (response) => {
          localStorage.setItem('token', response.token);
          this.loading.set(false);
          this.router.navigate(['/']);
        },
        error: (err) => {
          console.error(err);
          this.error.set('Incorrect email or password. Please try again.');
          this.loading.set(false);
        },
      });
  }
}