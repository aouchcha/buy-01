import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../core/services/auth'
import { Role } from '../../core/models/user'
import { CartService } from '../../core/services/cart';
import { MatIconModule } from '@angular/material/icon';


@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit {

  private readonly authService = inject(Auth);
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService);
  readonly Role = Role;
  readonly cartItemCount = this.cartService.itemCount;


  readonly isMenuOpen = signal(false);
  readonly isLogin = signal(false);
  readonly isSeller = signal(false)

  ngOnInit(): void {
    const token = localStorage.getItem('token');
    this.isLogin.set(!!token);
    const role = this.authService.getUserRole()
    console.log(role);

    this.isSeller.set(Role.SELLER === role)

    if (token) {
      this.cartService.load().subscribe({ error: () => {} });
    }
  }

  toggleMenu(): void {
    this.isMenuOpen.update((value) => !value);
  }

  closeMenu(): void {
    this.isMenuOpen.set(false);
  }

  // openCart(): void {
  //   this.closeMenu();
  //   this.router.navigate(['/cart']);
  // }

  logout(): void {
    this.authService.logout();
    this.cartService.reset();
    this.isLogin.set(false);
    this.closeMenu();
    this.router.navigate(['/login']);
  }
}