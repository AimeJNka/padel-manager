import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Paiement } from '../../../core/models/paiement.model';
import { PaiementService } from '../../../core/services/paiement.service';

export interface AnnulationDialogResult {
  success: boolean;
  error?: HttpErrorResponse;
}

const TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;

@Component({
  selector: 'app-annulation-dialog',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './annulation-dialog.html',
  styles: [`
    .msg-box {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      padding: 12px 14px;
      border-radius: 6px;
      border-left: 4px solid;
      margin-bottom: 8px;
    }
    .msg-box p { margin: 0; line-height: 1.5; }
    .msg-box--warn {
      border-color: #d32f2f;
      background: rgba(211, 47, 47, 0.06);
    }
    .msg-box--ok {
      border-color: #388e3c;
      background: rgba(56, 142, 60, 0.06);
    }
    .msg-icon { flex-shrink: 0; margin-top: 1px; }
    .msg-icon--ok { color: #388e3c; }
  `],
})
export class AnnulationDialog {

  private readonly dialogRef =
    inject<MatDialogRef<AnnulationDialog, AnnulationDialogResult>>(MatDialogRef);
  private readonly paiementService = inject(PaiementService);
  readonly data = inject<Paiement>(MAT_DIALOG_DATA);

  readonly isSubmitting = signal(false);

  /** True when the match starts in 24h or more — refund/cancel is free. */
  readonly isEarlyCancel = computed<boolean>(() => {
    const start = new Date(this.data.matchDateHeureDebut).getTime();
    return start - Date.now() >= TWENTY_FOUR_HOURS_MS;
  });

  /** Pre-formatted match date (e.g. "12/05/2026 à 14:00"). */
  readonly matchDateLabel = computed<string>(() => {
    const pipe = new DatePipe('fr-BE');
    return pipe.transform(this.data.matchDateHeureDebut, "dd/MM/yyyy 'à' HH:mm") ?? '';
  });

  cancel(): void {
    if (this.isSubmitting()) {
      return;
    }
    this.dialogRef.close({ success: false });
  }

  confirm(): void {
    if (this.isSubmitting()) {
      return;
    }
    this.isSubmitting.set(true);
    this.paiementService.annulerParticipation(this.data.idMatch).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.dialogRef.close({ success: true });
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.dialogRef.close({ success: false, error: err });
      },
    });
  }
}
