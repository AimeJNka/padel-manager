import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-redirect',
  template: '',
})
export class DashboardRedirect implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const role = this.authService.getRole();

    switch (role) {
      case 'GLOBAL':
        this.router.navigate(['/dashboard/membre-global']);
        break;
      case 'SITE':
        this.router.navigate(['/dashboard/membre-site']);
        break;
      case 'LIBRE':
        this.router.navigate(['/dashboard/membre-libre']);
        break;
      case 'ADMIN_GLOBAL':
      case 'ADMIN_SITE':
        this.router.navigate(['/dashboard/admin']);
        break;
      default:
        this.router.navigate(['/login']);
        break;
    }
  }
}
