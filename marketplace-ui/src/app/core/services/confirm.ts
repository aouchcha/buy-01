import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

import { ConfirmDialog, ConfirmDialogData } from '../../shared/components/confirm/confirm';

@Injectable({
  providedIn: 'root',
})
export class ConfirmService {
  private readonly dialog = inject(MatDialog);

  open(data: ConfirmDialogData): Observable<boolean> {
    const ref = this.dialog.open(ConfirmDialog, {
      data,
      width: '360px',
    });

    return ref.afterClosed().pipe(map((result) => result === true));
  }
}