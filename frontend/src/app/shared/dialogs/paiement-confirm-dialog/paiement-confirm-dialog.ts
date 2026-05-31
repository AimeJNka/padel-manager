import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface PaiementConfirmData {
  montant:   number;
  soldeDu:   number;
  matchDate: string;
}

@Component({
  selector: 'app-paiement-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, MatDialogModule, MatButtonModule],
  templateUrl: './paiement-confirm-dialog.html',
})
export class PaiementConfirmDialog {
  protected readonly data      = inject<PaiementConfirmData>(MAT_DIALOG_DATA);
  protected readonly dialogRef = inject(MatDialogRef<PaiementConfirmDialog, boolean>);

  protected get total(): number {
    return this.data.montant + this.data.soldeDu;
  }

  protected confirm(): void { this.dialogRef.close(true);  }
  protected cancel():  void { this.dialogRef.close(false); }
}
