import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { AuthService } from '../../../core/services/auth.service';
import { SiteService, Site } from '../../../core/services/site.service';
import { TabHoraires } from './tab-horaires';

type Tab = 'horaires' | 'recurrentes' | 'ponctuelles';

@Component({
  selector: 'app-gestion-horaires',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, ReactiveFormsModule, RouterLink, TabHoraires],
  templateUrl: './gestion-horaires.html',
})
export class GestionHoraires implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly siteService = inject(SiteService);

  protected readonly isGlobal = computed(() => this.auth.role() === 'ADMIN_GLOBAL');
  protected readonly sites = signal<Site[]>([]);
  protected readonly activeTab = signal<Tab>('horaires');

  protected readonly siteSelector = this.fb.control<number | null>(null);
  protected readonly selectedSiteId = toSignal(this.siteSelector.valueChanges, { initialValue: null as number | null });

  protected readonly currentSiteName = computed(() => {
    const id = this.selectedSiteId();
    if (id == null) return '';
    return this.sites().find(s => s.idSite === id)?.nom ?? '';
  });

  ngOnInit(): void {
    this.siteService.getSites().subscribe({
      next: (sites) => {
        this.sites.set(sites);
        if (!this.isGlobal()) {
          const id = this.auth.idSite();
          if (id != null) this.siteSelector.setValue(id);
        }
      },
      error: () => { /* silent — selector stays empty, user sees default placeholder */ },
    });
  }

  setActiveTab(t: Tab): void {
    this.activeTab.set(t);
  }
}
