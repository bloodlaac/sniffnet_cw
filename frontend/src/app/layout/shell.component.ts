import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div>
          <a class="brand" routerLink="/dashboard">
            <span class="brand-badge">SN</span>
            <div>
              <strong>SniffNet</strong>
              <p>Food freshness monitor</p>
            </div>
          </a>

          <nav class="nav">
            @for (item of navigation(); track item.link) {
              <a
                [routerLink]="item.link"
                routerLinkActive="active"
                class="nav-link"
              >
                <span>{{ item.icon }}</span>
                <span>{{ item.label }}</span>
              </a>
            }
          </nav>
        </div>

        <div class="profile-card">
          <p class="profile-label">Текущий пользователь</p>
          <strong>{{ auth.currentUser()?.username }}</strong>
          <p>{{ auth.currentUser()?.email }}</p>
          <span class="role-chip">{{ auth.currentUser()?.role }}</span>
          <button type="button" class="ghost-button" (click)="logout()">Выйти</button>
        </div>
      </aside>

      <main class="content">
        <header class="topbar">
          <div>
            <h1>Оценка свежести продуктов по фото</h1>
          </div>
        </header>

        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
    }

    .shell {
      display: grid;
      grid-template-columns: 280px minmax(0, 1fr);
      min-height: 100dvh;
    }

    .sidebar {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      gap: 2rem;
      padding: 1.5rem;
      background: rgba(16, 35, 31, 0.92);
      border-right: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(18px);
      position: sticky;
      top: 0;
      height: 100dvh;
    }

    .brand {
      display: flex;
      gap: 1rem;
      align-items: center;
      color: var(--color-text-strong);
      text-decoration: none;
      margin-bottom: 2rem;
    }

    .brand strong {
      font-size: 1.15rem;
      display: block;
    }

    .brand p,
    .profile-card p {
      margin: 0.2rem 0 0;
      color: var(--color-text-soft);
      font-size: 0.92rem;
    }

    .brand-badge {
      display: grid;
      place-items: center;
      width: 3rem;
      height: 3rem;
      border-radius: 1rem;
      background: linear-gradient(135deg, #f0c66e, #f57f56);
      color: #13231f;
      font-weight: 700;
    }

    .nav {
      display: grid;
      gap: 0.5rem;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: 0.8rem;
      padding: 0.9rem 1rem;
      border-radius: 1rem;
      color: var(--color-text-soft);
      text-decoration: none;
      transition: background-color 160ms ease, color 160ms ease, transform 160ms ease;
    }

    .nav-link:hover,
    .nav-link.active {
      background: rgba(255, 255, 255, 0.09);
      color: var(--color-text-strong);
      transform: translateX(2px);
    }

    .profile-card {
      padding: 1rem;
      border-radius: 1.25rem;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .profile-label {
      text-transform: uppercase;
      letter-spacing: 0.08em;
      font-size: 0.75rem;
      margin-bottom: 0.5rem;
    }

    .role-chip {
      display: inline-flex;
      margin-top: 0.9rem;
      padding: 0.35rem 0.65rem;
      border-radius: 999px;
      background: rgba(240, 198, 110, 0.16);
      color: #f6c36d;
      font-size: 0.82rem;
    }

    .ghost-button {
      width: 100%;
      margin-top: 1rem;
    }

    .content {
      padding: 1.75rem;
    }

    .topbar {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: 1rem;
      margin-bottom: 1.75rem;
    }

    .topbar h1 {
      margin: 0.2rem 0 0;
      font-size: clamp(2rem, 3.3vw, 3.2rem);
    }

    .eyebrow {
      margin: 0;
      color: var(--color-accent);
      text-transform: uppercase;
      letter-spacing: 0.12em;
      font-size: 0.72rem;
    }

    .topbar-note {
      max-width: 24rem;
      margin: 0;
      color: var(--color-text-muted);
    }

    @media (max-width: 980px) {
      .shell {
        grid-template-columns: 1fr;
      }

      .sidebar {
        position: static;
        height: auto;
      }

      .topbar {
        flex-direction: column;
        align-items: flex-start;
      }
    }
  `
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly navigation = computed(() => {
    const items = [
      { label: 'Обзор', link: '/dashboard', icon: '01' },
      { label: 'Эксперименты', link: '/experiments', icon: '02' },
      { label: 'Классификация', link: '/classification', icon: '03' },
      { label: 'История', link: '/history', icon: '04' }
    ];

    if (this.auth.isAdmin()) {
      items.push({ label: 'Пользователи', link: '/admin/users', icon: '05' });
    }

    return items;
  });

  constructor() {
    if (this.auth.isAuthenticated()) {
      this.auth.loadCurrentUser().pipe(catchError(() => of(null))).subscribe();
    }
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/auth']);
  }
}
