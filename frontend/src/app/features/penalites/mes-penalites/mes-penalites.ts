import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { PenaliteService } from '../../../core/services/penalite.service';
import { Penalite } from '../../../core/models/penalite.model';
import { PageShell } from '../../../shared/components/page-shell/page-shell';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';

@Component({
  selector: 'app-mes-penalites',
  imports: [DatePipe, PageShell, StatusBadge],
  templateUrl: './mes-penalites.html',
})
export class MesPenalites implements OnInit {

  private readonly penaliteService = inject(PenaliteService);

  readonly penalites = signal<Penalite[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string>('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.penaliteService.getMesPenalites().subscribe({
      next: (data) => {
        this.penalites.set(data);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.error ?? 'Erreur lors du chargement.');
      },
    });
  }
}
