import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit {
  readonly isMenuOpen = signal(false);
  readonly isLogin = signal(false);

  ngOnInit(): void {
    const token = localStorage.getItem('token');
    this.isLogin.set(!!token);
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