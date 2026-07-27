import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../core/services/auth'
import { Role } from '../../core/models/user'


@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit {

  private readonly authService = inject(Auth);
  private readonly router = inject(Router);
  readonly Role = Role;


  readonly isMenuOpen = signal(false);
  readonly isLogin = signal(false);
  readonly isSeller = signal(false)

  ngOnInit(): void {
    const token = localStorage.getItem('token');
    this.isLogin.set(!!token);
    const role = this.authService.getUserRole()
    console.log(role);
    
    this.isSeller.set(Role.SELLER === role)

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
    this.authService.logout();
    this.isLogin.set(false);
    this.closeMenu();
    this.router.navigate(['/login']);
  }
}