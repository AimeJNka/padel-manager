import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { MatchPadelDTO } from '../../../core/models/match.model';
import { StatusBadge } from '../status-badge/status-badge';

@Component({
  selector: 'app-match-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, CurrencyPipe, StatusBadge, RouterLink],
  templateUrl: './match-card.html',
})
export class MatchCard {
  readonly match = input.required<MatchPadelDTO>();
  readonly linkTo = input<string | null>(null);
}
