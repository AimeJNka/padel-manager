import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { MatchService } from '../../core/services/match.service';
import { MatchPadelDTO } from '../../core/models/match.model';
import { PageShell } from '../../shared/components/page-shell/page-shell';
import { MatchCard } from '../../shared/components/match-card/match-card';

@Component({
  selector: 'app-historique',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, MatchCard],
  templateUrl: './historique.html',
})
export class Historique {

  private readonly allMatches = toSignal(
    inject(MatchService).lister({ mine: true, size: 50, includeAnnulee: true }).pipe(map(p => p.content)),
    { initialValue: [] as MatchPadelDTO[] },
  );

  readonly pastMatches = computed(() =>
    this.allMatches()
      .filter(m => m.statut === 'EFFECTUE' || m.statut === 'ANNULE')
      .sort((a, b) =>
        new Date(b.disponibilite.dateHeureDebut).getTime() -
        new Date(a.disponibilite.dateHeureDebut).getTime()
      )
  );
}
