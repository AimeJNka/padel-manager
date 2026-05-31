import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatRadioChange, MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { DatePipe } from '@angular/common';

import { DisponibiliteDTO } from '../../../core/models/match.model';
import { MembreSearchDTO } from '../../../core/models/membre.model';
import { MemberPicker } from '../../components/member-picker/member-picker';
import { AuthService } from '../../../core/services/auth.service';

export interface CreerMatchDialogData {
  slot: DisponibiliteDTO;
  shareEstimate: number;
}

export type MatchType = 'PRIVE' | 'PUBLIC';

export interface CreerMatchDialogResult {
  type: MatchType;
  invites?: MembreSearchDTO[];
}

@Component({
  selector: 'app-creer-match-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatRadioModule, MatButtonModule, DatePipe, MemberPicker],
  templateUrl: './creer-match-dialog.html',
})
export class CreerMatchDialog {
  protected readonly data      = inject<CreerMatchDialogData>(MAT_DIALOG_DATA);
  protected readonly dialogRef = inject(MatDialogRef<CreerMatchDialog, CreerMatchDialogResult>);
  private   readonly auth      = inject(AuthService);

  protected readonly selectedType        = signal<MatchType>('PRIVE');
  protected readonly invites             = signal<MembreSearchDTO[]>([]);
  protected readonly excludedMatricules  = computed(() => {
    const m = this.auth.matricule();
    return m ? [m] : [];
  });

  protected readonly helperText = computed(() =>
    this.selectedType() === 'PRIVE'
      ? 'Vous serez seul organisateur. Invitez jusqu\'à 3 joueurs ci-dessous.'
      : "D'autres joueurs pourront s'inscrire via la page Matchs publics."
  );

  protected onTypeChange(change: MatRadioChange): void {
    this.selectedType.set(change.value as MatchType);
  }

  protected onConfirm(): void {
    const result: CreerMatchDialogResult = { type: this.selectedType() };
    if (this.selectedType() === 'PRIVE' && this.invites().length > 0) {
      result.invites = this.invites();
    }
    this.dialogRef.close(result);
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}
