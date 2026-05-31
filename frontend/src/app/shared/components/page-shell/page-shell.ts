import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  HostListener,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs/operators';
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
  private readonly router = inject(Router);

  protected readonly matricule = this.auth.matricule;
  protected readonly canCreateMatch = computed(() => {
    const role = this.auth.role();
    return role === 'GLOBAL' || role === 'SITE';
  });
  protected readonly isAdmin = computed(() => {
    const role = this.auth.role();
    return role === 'ADMIN_GLOBAL' || role === 'ADMIN_SITE';
  });

  // Mobile drawer state. Initial value is false on every page load — the static
  // sidebar at md+ ignores this signal because of `md:translate-x-0` in the template.
  protected readonly mobileMenuOpen = signal(false);

  constructor() {
    // Auto-close the drawer when navigation completes. Subscribing here (no ngOnInit)
    // is safe because takeUntilDestroyed() ties the subscription to the component's
    // destruction. Works for every routerLink click inside the nav.
    this.router.events
      .pipe(
        filter(e => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.closeMenu());

    // Body scroll-lock follows the drawer state. Set inside an effect() so the
    // sync is reactive and cleanup-free (Angular tears the effect down on destroy).
    effect(() => {
      document.body.style.overflow = this.mobileMenuOpen() ? 'hidden' : '';
    });
  }

  // Escape closes the drawer from anywhere on the page. Filter on the signal value
  // to avoid swallowing Escape when the drawer is not open (other components or
  // overlays may want it).
  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    if (this.mobileMenuOpen()) this.closeMenu();
  }

  protected toggleMenu(): void { this.mobileMenuOpen.update(v => !v); }
  protected openMenu(): void  { this.mobileMenuOpen.set(true); }
  protected closeMenu(): void { this.mobileMenuOpen.set(false); }

  logout(): void {
    this.auth.logout();
  }
}
