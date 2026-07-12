import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly snackBar = inject(MatSnackBar);

  private show(message: string, panelClass: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3500,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['app-toast', panelClass],
    });
  }

  success(message: string): void {
    this.show(message, 'app-toast--success');
  }

  error(message: string): void {
    this.show(message, 'app-toast--error');
  }

  info(message: string): void {
    this.show(message, 'app-toast--info');
  }
}