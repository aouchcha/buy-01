import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  isMenuOpen =  signal(false);

  

  toggleMenu(): void {
    this.isMenuOpen.set(!this.isMenuOpen())
  }

  closeMenu(): void {
    this.isMenuOpen.set(false);
  }

  opneCart(): void {
    this.closeMenu();

  }
}