import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  template: `
    <article class="bg-white rounded-xl border border-ink-100 p-4 shadow-xs">
      <div class="flex items-start justify-between mb-3">
        <span class="text-xs text-ink-500 uppercase tracking-wide font-medium">{{ label() }}</span>
        <mat-icon class="!text-[18px] !h-[18px] !w-[18px] text-padel-blue">{{ icon() }}</mat-icon>
      </div>
      <div class="text-2xl font-semibold text-padel-ink tabular-nums">{{ value() }}</div>
      @if (sub()) {
        <div class="text-xs text-ink-400 mt-1">{{ sub() }}</div>
      }
    </article>
  `,
})
export class KpiCard {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly icon = input.required<string>();
  readonly sub = input<string | undefined>(undefined);
}
