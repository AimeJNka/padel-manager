import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatRadioChange, MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { DatePipe } from '@angular/common';

import { DisponibiliteDTO } from '../../../core/models/match.model';

export interface CreerMatchDialogData {
  slot: DisponibiliteDTO;
  shareEstimate: number;
}

export type MatchType = 'PRIVE' | 'PUBLIC';

export interface CreerMatchDialogResult {
  type: MatchType;
}

@Component({
  selector: 'app-creer-match-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatRadioModule, MatButtonModule, DatePipe],
  templateUrl: './creer-match-dialog.html',
})
export class CreerMatchDialog {
  protected readonly data      = inject<CreerMatchDialogData>(MAT_DIALOG_DATA);
  protected readonly dialogRef = inject(MatDialogRef<CreerMatchDialog, CreerMatchDialogResult>);

  protected readonly selectedType = signal<MatchType>('PRIVE');

  protected readonly helperText = computed(() =>
    this.selectedType() === 'PRIVE'
      ? 'Vous serez seul organisateur. Vous pourrez inviter des joueurs depuis le match.'
      : "D'autres joueurs pourront s'inscrire via la page Matchs publics."
  );

  protected onTypeChange(change: MatRadioChange): void {
    this.selectedType.set(change.value as MatchType);
  }

  protected onConfirm(): void {
    this.dialogRef.close({ type: this.selectedType() });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}
