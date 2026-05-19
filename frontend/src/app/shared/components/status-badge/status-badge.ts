import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span [class]="classes()">
      <span [class]="dotClass()"></span>
      {{ label() }}
    </span>
  `,
})
export class StatusBadge {
  readonly status = input.required<string>();

  readonly classes = computed(() => {
    const base = 'inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium';
    const colorMap: Record<string, string> = {
      EN_ATTENTE: 'bg-warning/10 text-warning',
      CONFIRME:   'bg-success/10 text-success',
      PAYE:       'bg-success/10 text-success',
      LIBRE:      'bg-success/10 text-success',
      ANNULE:     'bg-error/10 text-error',
      TERMINE:    'bg-ink-100 text-ink-500',
      RESERVE:    'bg-padel-blue/10 text-padel-blue',
      REMBOURSE:  'bg-padel-blue/10 text-padel-blue',
    };
    const color = colorMap[this.status()] ?? 'bg-ink-100 text-ink-500';
    return `${base} ${color}`;
  });

  readonly dotClass = computed(() => {
    const dotMap: Record<string, string> = {
      EN_ATTENTE: 'w-1.5 h-1.5 rounded-full bg-warning',
      CONFIRME:   'w-1.5 h-1.5 rounded-full bg-success',
      PAYE:       'w-1.5 h-1.5 rounded-full bg-success',
      LIBRE:      'w-1.5 h-1.5 rounded-full bg-success',
      ANNULE:     'w-1.5 h-1.5 rounded-full bg-error',
      TERMINE:    'w-1.5 h-1.5 rounded-full bg-ink-400',
      RESERVE:    'w-1.5 h-1.5 rounded-full bg-padel-blue',
      REMBOURSE:  'w-1.5 h-1.5 rounded-full bg-padel-blue',
    };
    return dotMap[this.status()] ?? 'w-1.5 h-1.5 rounded-full bg-ink-400';
  });

  readonly label = computed(() => {
    const labelMap: Record<string, string> = {
      EN_ATTENTE: 'En attente',
      CONFIRME:   'Confirmé',
      PAYE:       'Payé',
      LIBRE:      'Libre',
      ANNULE:     'Annulé',
      TERMINE:    'Terminé',
      RESERVE:    'Réservé',
      REMBOURSE:  'Remboursé',
    };
    return labelMap[this.status()] ?? this.status();
  });
}
