import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { formatApiError } from '../core/api.utils';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="auth-layout">
      <div class="auth-copy card">
        <p class="eyebrow">SniffNet</p>
        <h1>Здесь только свежее</h1>

        <div class="feature-list">
          <div>
            <strong>Эксперименты</strong>
            <p>Создание экспериментов с различными конфигурациями.</p>
          </div>
          <div>
            <strong>Выбор моделей</strong>
            <p>Выбор модели, подходящей для вашей задачи.</p>
          </div>
          <div>
            <strong>Определение свежести</strong>
            <p>Определение свежести продуктов с высокой вероятностью.</p>
          </div>
        </div>
      </div>

      <div class="auth-panel card">
        <div class="tabs">
          <button type="button" [class.active]="mode() === 'login'" (click)="mode.set('login')">
            Вход
          </button>
          <button type="button" [class.active]="mode() === 'register'" (click)="mode.set('register')">
            Регистрация
          </button>
        </div>

        @if (error()) {
          <div class="error-banner">{{ error() }}</div>
        }

        @if (mode() === 'login') {
          <form class="form-grid" [formGroup]="loginForm" (ngSubmit)="submitLogin()">
            <label>
              Username
              <input type="text" formControlName="username" placeholder="demo" />
            </label>
            <label>
              Пароль
              <input type="password" formControlName="password" placeholder="demo123" />
            </label>
            <button type="submit" class="primary-button" [disabled]="submitting()">
              {{ submitting() ? 'Входим...' : 'Войти' }}
            </button>
          </form>
        } @else {
          <form class="form-grid" [formGroup]="registerForm" (ngSubmit)="submitRegister()">
            <label>
              Username
              <input type="text" formControlName="username" placeholder="student" />
            </label>
            <label>
              Email
              <input type="email" formControlName="email" placeholder="student@sniffnet.local" />
            </label>
            <label>
              Пароль
              <input type="password" formControlName="password" placeholder="minimum 6 symbols" />
            </label>
            <button type="submit" class="primary-button" [disabled]="submitting()">
              {{ submitting() ? 'Создаем...' : 'Создать аккаунт' }}
            </button>
          </form>
        }

        <div class="hint">
          <strong>Тестовые аккаунты:</strong> admin / admin123, demo / demo123
        </div>
      </div>
    </section>
  `,
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
    }

    .auth-layout {
      min-height: 100dvh;
      display: grid;
      grid-template-columns: 1.1fr 0.9fr;
      gap: 1.5rem;
      padding: 1.5rem;
      align-items: stretch;
    }

    .auth-copy,
    .auth-panel {
      padding: 2rem;
      min-height: 36rem;
    }

    .auth-copy {
      background:
        radial-gradient(circle at top right, rgba(240, 198, 110, 0.16), transparent 28rem),
        linear-gradient(160deg, rgba(4, 79, 63, 0.95), rgba(21, 48, 43, 0.94));
    }

    .auth-copy h1 {
      font-size: clamp(2.4rem, 6vw, 4.8rem);
      margin: 0.5rem 0 1rem;
      max-width: 10ch;
    }

    .auth-copy > p {
      max-width: 38rem;
      color: var(--color-text-soft);
      font-size: 1.05rem;
    }

    .feature-list {
      display: grid;
      gap: 1rem;
      margin-top: 2rem;
    }

    .feature-list div {
      padding: 1rem;
      border-radius: 1.1rem;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .feature-list p {
      margin: 0.35rem 0 0;
      color: var(--color-text-soft);
    }

    .auth-panel {
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 1.2rem;
    }

    .tabs {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      background: rgba(16, 35, 31, 0.06);
      padding: 0.3rem;
      border-radius: 999px;
    }

    .tabs button {
      border: 0;
      background: transparent;
      padding: 0.85rem 1rem;
      border-radius: 999px;
      color: var(--color-text-muted);
    }

    .tabs button.active {
      background: #10231f;
      color: var(--color-text-strong);
    }

    .hint {
      margin-top: 0.5rem;
      color: var(--color-text-muted);
      font-size: 0.95rem;
    }

    @media (max-width: 960px) {
      .auth-layout {
        grid-template-columns: 1fr;
      }

      .auth-copy,
      .auth-panel {
        min-height: auto;
      }
    }
  `
})
export class AuthPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly mode = signal<'login' | 'register'>('login');
  readonly submitting = signal(false);
  readonly error = signal('');

  readonly loginForm = this.fb.nonNullable.group({
    username: ['demo', [Validators.required]],
    password: ['demo123', [Validators.required]]
  });

  readonly registerForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor() {
    if (this.auth.isAuthenticated()) {
      void this.router.navigate(['/dashboard']);
    }
  }

  submitLogin(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.error.set('');
    this.submitting.set(true);
    this.auth.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(formatApiError(err));
      }
    });
  }

  submitRegister(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.error.set('');
    this.submitting.set(true);
    this.auth.register(this.registerForm.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(formatApiError(err));
      }
    });
  }
}
