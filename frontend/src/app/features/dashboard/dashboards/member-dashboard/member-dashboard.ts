import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { map } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { MatchService } from '../../../../core/services/match.service';
import { PaiementService } from '../../../../core/services/paiement.service';
import { PenaliteService } from '../../../../core/services/penalite.service';
import { MembreService } from '../../../../core/services/membre.service';
import { MatchPadelDTO } from '../../../../core/models/match.model';
import { Paiement } from '../../../../core/models/paiement.model';
import { Penalite } from '../../../../core/models/penalite.model';

import { PageShell } from '../../../../shared/components/page-shell/page-shell';
import { KpiCard } from '../../../../shared/components/kpi-card/kpi-card';
import { MatchCard } from '../../../../shared/components/match-card/match-card';

@Component({
  selector: 'app-member-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, PageShell, KpiCard, MatchCard],
  templateUrl: './member-dashboard.html',
})
export class MemberDashboard {
  protected readonly auth = inject(AuthService);

  protected readonly allMatches = toSignal(
    inject(MatchService).lister({ mine: true, size: 50 }).pipe(map(p => p.content)),
    { initialValue: [] as MatchPadelDTO[] },
  );
  protected readonly paiements = toSignal(
    inject(PaiementService).getMesPaiements(),
    { initialValue: [] as Paiement[] },
  );
  protected readonly profil = toSignal(
    inject(MembreService).getMonProfil(),
    { initialValue: null },
  );
  protected readonly penalites = toSignal(
    inject(PenaliteService).getMesPenalites(),
    { initialValue: [] as Penalite[] },
  );

  protected readonly isLibre = computed(() => this.auth.role() === 'LIBRE');

  protected readonly upcomingMatches = computed(() => {
    const now = new Date();
    return this.allMatches()
      .filter(m =>
        (m.statut === 'EN_ATTENTE' || m.statut === 'CONFIRME') &&
        new Date(m.disponibilite.dateHeureDebut) > now
      )
      .sort((a, b) =>
        new Date(a.disponibilite.dateHeureDebut).getTime() -
        new Date(b.disponibilite.dateHeureDebut).getTime()
      )
      .slice(0, 5);
  });

  protected readonly pendingPaiementCount = computed(() =>
    this.paiements().filter(p => p.statut === 'EN_ATTENTE').length
  );

  protected readonly soldeDuFormatted = computed(() => {
    const m = this.profil();
    if (!m) return '—';
    return `€${m.soldeDu.toFixed(2)}`;
  });

  protected readonly penaliteActive = computed(() =>
    this.penalites().find(p => p.active) ?? null
  );

  protected readonly greeting = computed(() => {
    const m = this.profil();
    if (m?.prenom && m?.nom) {
      return `${m.prenom} ${m.nom}`;
    }
    return this.auth.matricule() ?? '';
  });
}
