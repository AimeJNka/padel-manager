import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { PageShell } from '../page-shell/page-shell';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-coming-soon',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell, MatIconModule],
  template: `
    <app-page-shell>
      <main class="p-6 max-w-3xl mx-auto">
        <div class="bg-white rounded-xl border border-ink-100 p-12 shadow-xs text-center">
          <mat-icon class="!text-5xl !h-12 !w-12 text-padel-blue mb-4">construction</mat-icon>
          <h1 class="text-xl font-semibold text-padel-ink mb-2">{{ title() }}</h1>
          <p class="text-sm text-ink-500">Cette section sera disponible prochainement.</p>
        </div>
      </main>
    </app-page-shell>
  `,
})
export class ComingSoon {
  readonly title = input<string>('Bientôt disponible');
}
