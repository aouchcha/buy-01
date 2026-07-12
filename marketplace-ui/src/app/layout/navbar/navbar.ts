import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Auth } from '../../core/services/auth'
import { Role } from '../../core/models/user'
import { Login } from '../../features/auth/pages/login/login';


@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit {

  private authService = inject(Auth);
  readonly Role = Role;


  readonly isMenuOpen = signal(false);
  readonly isLogin = signal(false);
  readonly isSeller = signal(false)

  ngOnInit(): void {
    const token = localStorage.getItem('token');
    this.isLogin.set(!!token);
    const role = this.authService.getUserRole()
    this.isSeller.set(Role.SELLER === role?.toLowerCase())

  }

  toggleMenu(): void {
    this.isMenuOpen.update((value) => !value);
  }

  closeMenu(): void {
    this.isMenuOpen.set(false);
  }

  openCart(): void {
    this.closeMenu();
  }

  logout(): void {
    localStorage.removeItem('token');
    this.isLogin.set(false);
    this.closeMenu();
  }
}