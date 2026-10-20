import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, catchError, forkJoin, map, merge, of, startWith, switchMap, tap, timer, finalize } from 'rxjs';
import { Exercise, ExerciseOptions, PageResult, OptionKey, optionLabel } from '../../models/exercise';
import { ExerciseService, apiError } from '../../services/exercise.service';
import { ExerciseEditorComponent } from './exercise-editor.component';

interface ListState { loading: boolean; error?: string; result?: PageResult<Exercise>; options?: ExerciseOptions; }
@Component({
  selector: 'app-exercise-list', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ExerciseEditorComponent],
  templateUrl: './exercise-list.component.html', styleUrls: ['./exercises.css']
})
export class ExerciseListComponent {
  @ViewChild('archivePanel') set archivePanel(panel: ElementRef<HTMLElement> | undefined) {
    if (panel) { panel.nativeElement.focus(); panel.nativeElement.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
  }
  private readonly api = inject(ExerciseService);
  private readonly fb = inject(FormBuilder).nonNullable;
  readonly admin = inject(ActivatedRoute).snapshot.data['admin'] === true;
  readonly filters = this.fb.group({ search: '', muscleGroup: '', equipment: '', difficulty: '', measurementType: '', status: 'ACTIVE', order: 'name,asc' });
  readonly selectFilters: { control: 'muscleGroup' | 'equipment' | 'difficulty' | 'measurementType'; label: string; key: OptionKey }[] = [
    { control: 'muscleGroup', label: 'Groupe musculaire', key: 'muscleGroups' },
    { control: 'equipment', label: 'Matériel', key: 'equipment' },
    { control: 'difficulty', label: 'Difficulté', key: 'difficulties' },
    { control: 'measurementType', label: 'Type de mesure', key: 'measurementTypes' }
  ];
  readonly label = optionLabel;
  readonly reload = new Subject<void>();
  page = 0;
  options?: ExerciseOptions;
  editorOpen = false;
  editing: Exercise | null = null;
  pendingArchive: Exercise | null = null;
  busy = false;
  message = '';
  actionError = '';
  readonly state$ = merge(
    this.filters.valueChanges.pipe(tap(() => { this.page = 0; }), map(() => true)),
    this.reload.pipe(map(() => false))
  ).pipe(startWith(false), switchMap(debounce => {
    const { order, ...query } = this.filters.getRawValue();
    const [sort, direction] = order.split(',');
    // Cancel both the debounce timer and the previous HTTP request on every change.
    return timer(debounce ? 300 : 0).pipe(switchMap(() => forkJoin({
      options: this.api.options$,
      result: this.api.list({ ...query, status: this.admin ? query.status : 'ACTIVE', sort, direction, page: this.page, size: this.admin ? 10 : 9 })
    })), tap(value => this.options = value.options), map(value => ({ ...value, loading: false } as ListState)),
      catchError(error => of({ loading: false, error: apiError(error) } as ListState)), startWith({ loading: true } as ListState));
  }));
  reset() { this.filters.reset(); }
  go(page: number) { this.page = page; this.reload.next(); }
  openEditor(exercise: Exercise | null = null) {
    this.editing = exercise; this.editorOpen = true; this.message = ''; this.actionError = ''; this.pendingArchive = null;
  }
  saved(message: string) { this.editorOpen = false; this.message = message; this.page = 0; this.reload.next(); }
  askArchive(exercise: Exercise) {
    this.message = ''; this.actionError = ''; this.pendingArchive = exercise;
  }
  changeStatus(exercise: Exercise) {
    if (this.busy) return;
    this.busy = true; this.message = ''; this.actionError = '';
    this.api.setArchived(exercise).pipe(finalize(() => this.busy = false)).subscribe({
      next: response => { this.message = response.message; this.pendingArchive = null; this.page = 0; this.reload.next(); },
      error: error => this.actionError = apiError(error)
    });
  }
}

