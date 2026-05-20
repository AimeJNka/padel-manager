import {
  ChangeDetectionStrategy, Component, OnInit,
  computed, inject, input, numberAttribute, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';

import { MatchService } from '../../../core/services/match.service';
import { AuthService } from '../../../core/services/auth.service';
import { MatchPadelDTO } from '../../../core/models/match.model';
import { MembreSearchDTO } from '../../../core/models/membre.model';
import { MemberPicker } from '../../../shared/components/member-picker/member-picker';
import { PageShell } from '../../../shared/components/page-shell/page-shell';

@Component({
  selector: 'app-inviter-joueurs',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, MemberPicker, PageShell],
  templateUrl: './inviter-joueurs.html',
})
export class InviterJoueurs implements OnInit {
  readonly id = input.required({ transform: numberAttribute });

  private readonly matchService = inject(MatchService);
  private readonly auth         = inject(AuthService);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly router       = inject(Router);

  readonly match        = signal<MatchPadelDTO | null>(null);
  readonly isLoading    = signal(true);
  readonly error        = signal<string | null>(null);
  readonly invites      = signal<MembreSearchDTO[]>([]);
  readonly isSubmitting = signal(false);

  private readonly currentMatricule = computed(() => this.auth.matricule());

  readonly isOrganizer = computed(() => {
    const cm = this.currentMatricule();
    return cm !== null && this.match()?.organisateur.matricule === cm;
  });

  readonly activeParticipations = computed(() =>
    this.match()?.participations.filter(p => p.statutParticipation !== 'ANNULEE') ?? []
  );

  readonly placesRestantes = computed(() => 4 - this.activeParticipations().length);

  readonly excludeMatricules = computed(() =>
    this.activeParticipations().map(p => p.matricule)
  );

  readonly siteId = computed(() =>
    this.match()?.disponibilite.terrain.site.idSite ?? null
  );

  readonly canInvite = computed(() => {
    const m = this.match();
    return m != null
      && this.isOrganizer()
      && m.typeMatch === 'PRIVE'
      && m.statut !== 'ANNULE'
      && this.placesRestantes() > 0;
  });

  readonly accessDeniedReason = computed((): string | null => {
    const m = this.match();
    if (!m) return null;
    if (!this.isOrganizer()) return "Seul l'organisateur peut inviter des joueurs.";
    if (m.typeMatch !== 'PRIVE') return "Les invitations ne sont possibles que pour les matchs privés.";
    if (m.statut === 'ANNULE') return "Ce match est annulé.";
    if (this.placesRestantes() <= 0) return "Ce match est complet.";
    return null;
  });

  ngOnInit(): void {
    this.fetchMatch();
  }

  fetchMatch(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.matchService.getOne(this.id()).subscribe({
      next: m  => { this.match.set(m); this.isLoading.set(false); },
      error: () => { this.error.set('Impossible de charger le match.'); this.isLoading.set(false); },
    });
  }

  async onSubmit(): Promise<void> {
    const selected = this.invites();
    if (selected.length === 0) return;

    this.isSubmitting.set(true);
    type InvitationResult = { invitee: MembreSearchDTO; success: boolean; error?: string };
    const results: InvitationResult[] = [];

    for (const invitee of selected) {
      try {
        await firstValueFrom(this.matchService.ajouterJoueur(this.id(), { matricule: invitee.matricule }));
        results.push({ invitee, success: true });
      } catch (err: any) {
        results.push({ invitee, success: false, error: err.error?.message ?? 'Erreur inconnue' });
      }
    }

    this.isSubmitting.set(false);
    this.handleResults(results);
  }

  private handleResults(results: Array<{ invitee: MembreSearchDTO; success: boolean; error?: string }>): void {
    const successes = results.filter(r => r.success).length;
    const failures  = results.filter(r => !r.success);
    const total     = results.length;

    let message: string;
    if (failures.length === 0) {
      message = `${total} invitation${total > 1 ? 's' : ''} envoyée${total > 1 ? 's' : ''}.`;
    } else if (successes === 0) {
      const detail = failures.map(f => `${f.invitee.prenom} ${f.invitee.nom}: ${f.error}`).join('; ');
      message = `Aucune invitation envoyée. ${detail}`;
    } else {
      const names = failures.map(f => `${f.invitee.prenom} ${f.invitee.nom}`).join(', ');
      message = `${successes}/${total} invitations envoyées. Échecs: ${names}.`;
    }

    this.snackBar.open(message, 'OK', { duration: 7000 });
    this.router.navigate(['/matchs', this.id()]);
  }
}
