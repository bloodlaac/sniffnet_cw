import { CommonModule, DecimalPipe, KeyValuePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Classification, Dataset, Model, PageResponse } from '../core/api.models';
import { formatApiError } from '../core/api.utils';

@Component({
  selector: 'app-classification-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DecimalPipe, KeyValuePipe],
  template: `
    @if (error()) {
      <div class="error-banner">{{ error() }}</div>
    }

    <section class="classification-grid">
      <article class="card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Inference</p>
            <h2>Проверка по фото</h2>
          </div>

          <label class="filter-select">
            <span>Датасет</span>
            <select [value]="datasetFilter()" (change)="selectDataset($any($event.target).value)">
              <option value="">Все</option>
              @for (dataset of datasets(); track dataset.id) {
                <option [value]="dataset.id">{{ dataset.name }}</option>
              }
            </select>
          </label>
        </div>

        <div class="model-list">
          @for (model of models(); track model.id) {
            <button
              type="button"
              class="model-card"
              [class.active]="selectedModelId() === model.id"
              (click)="pickModel(model.id)"
            >
              <div class="model-top">
                <strong>{{ model.name }}</strong>
                <span>{{ model.datasetName }}</span>
              </div>
              <div class="metric-line">
                <span>Val acc</span>
                <strong>
                  @if (model.metrics?.validationAccuracy) {
                    {{ model.metrics?.validationAccuracy | number: '1.2-2' }}
                  } @else {
                    n/a
                  }
                </strong>
              </div>
            </button>
          }
        </div>
      </article>

      <article class="card">
        <p class="eyebrow">Upload</p>
        <h2>Отправка изображения</h2>

        <form class="form-grid" [formGroup]="form" (ngSubmit)="submit()">
          <label>
            ID модели
            <input type="number" formControlName="modelId" min="1" />
          </label>

          <label class="upload-box">
            <input type="file" accept=".jpg,.jpeg,.png,image/png,image/jpeg" (change)="handleFile($event)" />
            <span>Выбрать JPG или PNG</span>
            <small>Максимум 5 MB</small>
          </label>

          @if (previewUrl()) {
            <img class="preview-image" [src]="previewUrl()" alt="preview" />
          }

          <button type="submit" class="primary-button" [disabled]="submitting()">
            {{ submitting() ? 'Отправляем...' : 'Запустить классификацию' }}
          </button>
        </form>

        @if (result(); as item) {
          <div class="result-box">
            <div class="result-head">
              <div>
                <p class="eyebrow">Result</p>
                <h3>{{ item.predictedClass || 'Нет класса' }}</h3>
              </div>
              <strong>
                @if (item.confidence) {
                  {{ item.confidence | number: '1.2-2' }}
                } @else {
                  n/a
                }
              </strong>
            </div>

            <div class="probability-list">
              @for (entry of item.probabilities | keyvalue; track entry.key) {
                <div class="probability-item">
                  <span>{{ entry.key }}</span>
                  <strong>{{ entry.value | number: '1.2-2' }}</strong>
                </div>
              }
            </div>

            <img class="result-image" [src]="api.assetUrl('/api/files/images/' + item.imageId + '/content')" alt="uploaded image" />
          </div>
        }
      </article>
    </section>
  `,
  styles: `
    .classification-grid {
      display: grid;
      grid-template-columns: 1.1fr 0.9fr;
      gap: 1rem;
    }

    .section-head {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      align-items: end;
      margin-bottom: 1rem;
    }

    .section-head h2 {
      margin: 0.25rem 0 0;
    }

    .model-list {
      display: grid;
      gap: 0.8rem;
    }

    .model-card {
      text-align: left;
      width: 100%;
      padding: 1rem;
      border-radius: 1rem;
      border: 1px solid rgba(16, 35, 31, 0.08);
      background: rgba(16, 35, 31, 0.03);
      transition: transform 160ms ease, border-color 160ms ease;
    }

    .model-card.active {
      border-color: rgba(245, 127, 86, 0.55);
      transform: translateY(-1px);
    }

    .model-top {
      display: grid;
      gap: 0.25rem;
      margin-bottom: 0.9rem;
    }

    .model-top span {
      color: var(--color-text-muted);
    }

    .metric-line,
    .result-head,
    .probability-item {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      align-items: center;
    }

    .upload-box {
      display: grid;
      place-items: center;
      gap: 0.3rem;
      min-height: 11rem;
      border: 1px dashed rgba(16, 35, 31, 0.18);
      border-radius: 1rem;
      background: rgba(16, 35, 31, 0.03);
      cursor: pointer;
      text-align: center;
    }

    .upload-box input {
      display: none;
    }

    .upload-box small {
      color: var(--color-text-muted);
    }

    .preview-image,
    .result-image {
      width: 100%;
      border-radius: 1rem;
      object-fit: cover;
      max-height: 18rem;
      background: rgba(16, 35, 31, 0.05);
    }

    .result-box {
      display: grid;
      gap: 1rem;
      margin-top: 1.5rem;
      padding-top: 1.5rem;
      border-top: 1px solid rgba(16, 35, 31, 0.08);
    }

    .result-head h3 {
      margin: 0.2rem 0 0;
    }

    .probability-list {
      display: grid;
      gap: 0.55rem;
    }

    .probability-item {
      padding: 0.8rem 0.9rem;
      border-radius: 0.95rem;
      background: rgba(16, 35, 31, 0.04);
    }

    @media (max-width: 1120px) {
      .classification-grid {
        grid-template-columns: 1fr;
      }
    }
  `
})
export class ClassificationPageComponent {
  readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly datasets = signal<Dataset[]>([]);
  readonly models = signal<Model[]>([]);
  readonly datasetFilter = signal('');
  readonly selectedModelId = signal<number | null>(null);
  readonly previewUrl = signal('');
  readonly submitting = signal(false);
  readonly error = signal('');
  readonly result = signal<Classification | null>(null);
  private selectedFile: File | null = null;

  readonly form = this.fb.nonNullable.group({
    modelId: [0, [Validators.required, Validators.min(1)]]
  });

  constructor() {
    this.loadDatasets();
    this.loadModels();
  }

  selectDataset(value: string): void {
    this.datasetFilter.set(value);
    this.loadModels(value ? Number(value) : undefined);
  }

  pickModel(modelId: number): void {
    this.selectedModelId.set(modelId);
    this.form.patchValue({ modelId });
  }

  handleFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile = file;
    this.previewUrl.set(file ? URL.createObjectURL(file) : '');
  }

  submit(): void {
    if (this.form.invalid || !this.selectedFile) {
      this.form.markAllAsTouched();
      this.error.set('Нужно выбрать модель и файл изображения.');
      return;
    }

    this.submitting.set(true);
    this.error.set('');
    this.result.set(null);

    const formData = new FormData();
    formData.append('modelId', String(this.form.getRawValue().modelId));
    formData.append('file', this.selectedFile);

    this.api.postFormData<Classification>('/classifications', formData).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.result.set(response);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(formatApiError(err));
      }
    });
  }

  private loadDatasets(): void {
    this.api.get<Dataset[]>('/datasets').subscribe({
      next: (response) => this.datasets.set(response),
      error: (err) => this.error.set(formatApiError(err))
    });
  }

  private loadModels(datasetId?: number): void {
    this.api.get<PageResponse<Model>>('/models', { size: 20, datasetId }).subscribe({
      next: (response) => this.models.set(response.content.filter((item) => item.availableForInference)),
      error: (err) => this.error.set(formatApiError(err))
    });
  }
}
