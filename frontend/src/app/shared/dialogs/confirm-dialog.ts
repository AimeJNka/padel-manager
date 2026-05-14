import { Component, inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      @if (data.destructive) {
        <button mat-button cdkFocusInitial (click)="onCancel()">
          {{ data.cancelLabel ?? 'Annuler' }}
        </button>
        <button mat-flat-button color="warn" (click)="onConfirm()">
          {{ data.confirmLabel ?? 'Confirmer' }}
        </button>
      } @else {
        <button mat-button (click)="onCancel()">
          {{ data.cancelLabel ?? 'Annuler' }}
        </button>
        <button mat-flat-button color="primary" cdkFocusInitial (click)="onConfirm()">
          {{ data.confirmLabel ?? 'Confirmer' }}
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content p { margin: 0; line-height: 1.6; }
    mat-dialog-actions { gap: 8px; }
  `],
})
export class ConfirmDialog {
  private readonly dialogRef =
    inject<MatDialogRef<ConfirmDialog, boolean>>(MatDialogRef);
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);

  onConfirm(): void { this.dialogRef.close(true); }
  onCancel(): void { this.dialogRef.close(false); }
}
