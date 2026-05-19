import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageShell } from '../../../shared/components/page-shell/page-shell';

@Component({
  selector: 'app-creer-match',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageShell],
  templateUrl: './creer-match.html',
})
export class CreerMatch {}
