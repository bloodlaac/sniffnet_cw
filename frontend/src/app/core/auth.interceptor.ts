import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';
import { AuthService, AuthSession } from './auth.service';

let refreshRequest$: Observable<AuthSession> | null = null;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  const isTokenEndpoint = isAuthTokenEndpoint(request.url);

  const authorizedRequest = token && !isTokenEndpoint
    ? request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : request;

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !auth.isAuthenticated() || isTokenEndpoint) {
        return throwError(() => error);
      }

      if (!auth.refreshToken()) {
        auth.logout();
        void router.navigate(['/auth']);
        return throwError(() => error);
      }

      refreshRequest$ ??= auth.refreshSession().pipe(
        shareReplay(1),
        finalize(() => {
          refreshRequest$ = null;
        })
      );

      return refreshRequest$.pipe(
        switchMap((session) =>
          next(
            request.clone({
              setHeaders: {
                Authorization: `Bearer ${session.token}`
              }
            })
          )
        ),
        catchError((refreshError) => {
          auth.logout();
          void router.navigate(['/auth']);
          return throwError(() => refreshError);
        })
      );
    })
  );
};

function isAuthTokenEndpoint(url: string): boolean {
  const path = url.split('?')[0];
  return (
    path.endsWith('/auth/login') ||
    path.endsWith('/auth/register') ||
    path.endsWith('/auth/refresh')
  );
}
