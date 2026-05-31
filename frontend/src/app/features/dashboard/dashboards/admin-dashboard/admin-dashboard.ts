import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { PageShell } from '../../../../shared/components/page-shell/page-shell';
import { KpiCard } from '../../../../shared/components/kpi-card/kpi-card';
import { AuthService } from '../../../../core/services/auth.service';
import { SiteService, Site } from '../../../../core/services/site.service';
import { MatchService } from '../../../../core/services/match.service';
import { PaiementService } from '../../../../core/services/paiement.service';
import { DisponibiliteService } from '../../../../core/services/disponibilite.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, KpiCard, RouterLink],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly siteService = inject(SiteService);
  private readonly matchService = inject(MatchService);
  private readonly paiementService = inject(PaiementService);
  private readonly disponibiliteService = inject(DisponibiliteService);
  private readonly http = inject(HttpClient);

  protected readonly isGlobal = computed(() => this.auth.role() === 'ADMIN_GLOBAL');
  protected readonly siteName = signal<string | null>(null);

  protected readonly greeting = computed(() => {
    if (this.isGlobal()) return 'Administrateur Global';
    const name = this.siteName();
    return name ? `Administrateur de ${name}` : 'Administrateur de site';
  });

  protected readonly kpi1Label = computed(() => (this.isGlobal() ? 'Membres' : 'Terrains'));
  protected readonly kpi1Icon = computed(() => (this.isGlobal() ? 'group' : 'sports_tennis'));

  protected readonly kpi1Value = signal<string | number>('—');
  protected readonly kpi2Value = signal<string | number>('—');
  protected readonly kpi3Value = signal<string | number>('—');
  protected readonly kpi4Value = signal<string | number>('—');

  ngOnInit(): void {
    this.loadSiteName();
    this.loadKpi1();
    this.loadKpi2();
    this.loadKpi3();
    this.loadKpi4();
  }

  private loadSiteName(): void {
    if (this.isGlobal()) return;
    const id = this.auth.idSite();
    if (id == null) return;
    this.siteService.getSites().subscribe({
      next: (sites: Site[]) => {
        const found = sites.find(s => s.idSite === id);
        if (found) this.siteName.set(found.nom);
      },
      error: () => { /* silent — greeting falls back */ },
    });
  }

  private loadKpi1(): void {
    if (this.isGlobal()) {
      this.http.get<unknown[]>('/api/membres').subscribe({
        next: (rows) => this.kpi1Value.set(rows.length),
        error: () => { /* silent */ },
      });
      return;
    }
    const id = this.auth.idSite();
    if (id == null) return;
    this.http.get<unknown[]>(`/api/sites/${id}/terrains`).subscribe({
      next: (rows) => this.kpi1Value.set(rows.length),
      error: () => { /* silent */ },
    });
  }

  private loadKpi2(): void {
    const siteId = this.isGlobal() ? null : this.auth.idSite();
    this.matchService.lister({ statut: 'EN_ATTENTE', siteId, size: 1 }).subscribe({
      next: (page) => this.kpi2Value.set(page.totalElements),
      error: () => { /* silent */ },
    });
  }

  private loadKpi3(): void {
    const siteId = this.isGlobal() ? null : this.auth.idSite();
    this.disponibiliteService.lister({ statut: 'LIBRE', siteId, size: 1 }).subscribe({
      next: (page) => this.kpi3Value.set(page.totalElements),
      error: () => { /* silent */ },
    });
  }

  private loadKpi4(): void {
    const siteId = this.isGlobal() ? null : this.auth.idSite();
    this.paiementService.getAllPaiements({ statut: 'EN_ATTENTE', siteId, size: 1 }).subscribe({
      next: (page) => this.kpi4Value.set(page.totalElements),
      error: () => { /* silent */ },
    });
  }
}
