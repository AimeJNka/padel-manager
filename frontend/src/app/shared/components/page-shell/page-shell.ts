import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { AuthService } from '../../../core/services/auth.service';

/**
 * Page shell for authenticated member space pages.
 *
 * Wraps any feature page with: persistent sidebar nav, member identity
 * footer, logout button, and a <ng-content /> slot for page content.
 *
 * Active route state uses a component-scoped `.is-active` class rather
 * than dynamic Tailwind classes in [routerLinkActive] — Tailwind v4's
 * static scanner does not reliably tokenize dynamic class strings inside
 * attribute values. The `color-mix(in srgb, var(--color-padel-blue) 8%, transparent)`
 * tint mirrors Tailwind's `bg-padel-blue/8` opacity modifier exactly.
 *
 * Usage:
 *   <app-page-shell>
 *     <h1>My Feature Page</h1>
 *     ...page content...
 *   </app-page-shell>
 */
@Component({
  selector: 'app-page-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, MatIconModule],
  templateUrl: './page-shell.html',
  styles: [`
    .nav-link.is-active {
      background-color: color-mix(in srgb, var(--color-padel-blue) 8%, transparent);
      color: var(--color-padel-blue);
      font-weight: 600;
    }
  `],
})
export class PageShell {
  protected readonly auth = inject(AuthService);
  protected readonly matricule = this.auth.matricule;
  protected readonly canCreateMatch = computed(() => {
    const role = this.auth.role();
    return role === 'GLOBAL' || role === 'SITE';
  });
  protected readonly isAdmin = computed(() => {
    const role = this.auth.role();
    return role === 'ADMIN_GLOBAL' || role === 'ADMIN_SITE';
  });

  logout(): void {
    this.auth.logout();
  }
}
