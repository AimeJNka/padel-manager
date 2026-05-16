// Refresh lock: a module-level Promise prevents multiple concurrent /api/auth/refresh calls.
// All 401 handlers that fire in parallel share the same Promise and replay their original
// request together once refresh resolves. The lock resets to null in .finally() so the
// next expiry cycle starts fresh.

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

interface PublicRoute {
  url: string;
  methods?: string[];
  exact?: boolean;
}

// Requests in this list bypass token attachment and 401 retry entirely.
const PUBLIC_ROUTES: PublicRoute[] = [
  { url: '/api/auth/login' },
  { url: '/api/auth/register' },
  { url: '/api/auth/admin/login' },
  { url: '/api/auth/refresh' },
  { url: '/api/types-membres', methods: ['GET'], exact: true },
  { url: '/api/sites', methods: ['GET'], exact: true },
];

// Requests here receive the token but are never retried on 401.
// Logout is excluded to prevent a retry loop: by the time its 401 arrives,
// the session is already cleared and refresh() would immediately return false,
// triggering another logout call indefinitely.
const NO_RETRY_ROUTES = ['/api/auth/logout'];

let refreshPromise: Promise<boolean> | null = null;

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  if (isPublicRequest(req.method, req.url)) {
    return next(req);
  }

  const token = authService.getToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      const isNoRetry = NO_RETRY_ROUTES.some(route => req.url.includes(route));
      if (err.status !== 401 || isNoRetry || req.headers.has('X-Auth-Retry')) {
        return throwError(() => err);
      }

      if (!refreshPromise) {
        refreshPromise = authService.refresh().finally(() => { refreshPromise = null; });
      }

      return from(refreshPromise).pipe(
        switchMap(ok => {
          if (!ok) {
            authService.logout();
            return throwError(() => err);
          }
          const newToken = authService.getToken()!;
          const retryHeaders = req.headers
            .set('Authorization', `Bearer ${newToken}`)
            .set('X-Auth-Retry', '1');
          return next(req.clone({ headers: retryHeaders }));
        })
      );
    })
  );
};

function isPublicRequest(method: string, url: string): boolean {
  const path = url.split('?')[0];
  return PUBLIC_ROUTES.some(route => {
    const methodMatches = !route.methods || route.methods.includes(method.toUpperCase());
    if (!methodMatches) return false;
    return route.exact ? path === route.url : url.includes(route.url);
  });
}
