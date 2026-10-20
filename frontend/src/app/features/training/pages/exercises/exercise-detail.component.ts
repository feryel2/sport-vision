import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, catchError, forkJoin, map, of, startWith, switchMap } from 'rxjs';
import { Exercise, ExerciseOptions, optionLabel } from '../../models/exercise';
import { ExerciseService, apiError } from '../../services/exercise.service';
interface DetailState { loading: boolean; error?: string; exercise?: Exercise; options?: ExerciseOptions; }
@Component({ selector: 'app-exercise-detail', standalone: true, imports: [CommonModule, RouterLink],
  templateUrl: './exercise-detail.component.html', styleUrls: ['./exercises.css'] })
export class ExerciseDetailComponent {
  private readonly api = inject(ExerciseService);
  private readonly route = inject(ActivatedRoute);
  readonly retry = new Subject<void>();
  readonly label = optionLabel;
  readonly state$ = this.route.paramMap.pipe(switchMap(params => this.retry.pipe(startWith(undefined), switchMap(() =>
    forkJoin({ exercise: this.api.get(params.get('id') ?? ''), options: this.api.options$ }).pipe(
      map(value => ({ ...value, loading: false } as DetailState)),
      catchError(error => of({ loading: false, error: apiError(error) } as DetailState)), startWith({ loading: true } as DetailState)
    )
  ))));
}
