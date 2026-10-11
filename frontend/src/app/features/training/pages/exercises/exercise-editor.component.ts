import { Component, AfterViewInit, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { Exercise, ExerciseInput, ExerciseOptions, OptionKey } from '../../models/exercise';
import { ExerciseService, apiError } from '../../services/exercise.service';

@Component({
  selector: 'app-exercise-editor', standalone: true, imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './exercise-editor.component.html', styleUrls: ['./exercises.css']
})
export class ExerciseEditorComponent implements OnInit, AfterViewInit {
  @Input({ required: true }) options!: ExerciseOptions;
  @Input() exercise: Exercise | null = null;
  @Output() saved = new EventEmitter<string>();
  @Output() closed = new EventEmitter<void>();
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly api = inject(ExerciseService);
  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120), this.nonBlank]],
    description: ['', [Validators.required, Validators.maxLength(2000), this.nonBlank]],
    instructions: ['', [Validators.required, Validators.maxLength(5000), this.nonBlank]],
    muscleGroup: ['', Validators.required], equipment: ['', Validators.required],
    difficulty: ['', Validators.required], measurementType: ['', Validators.required]
  });
  readonly selects: { control: 'muscleGroup' | 'equipment' | 'difficulty' | 'measurementType'; label: string; key: OptionKey }[] = [
    { control: 'muscleGroup', label: 'Groupe musculaire', key: 'muscleGroups' },
    { control: 'equipment', label: 'Matériel', key: 'equipment' },
    { control: 'difficulty', label: 'Difficulté', key: 'difficulties' },
    { control: 'measurementType', label: 'Type de mesure', key: 'measurementTypes' }
  ];
  @ViewChild('nameInput') nameInput!: ElementRef<HTMLInputElement>;
  ngAfterViewInit() { this.nameInput.nativeElement.focus(); }
  busy = false;
  error = '';
  serverErrors: Record<string, string> = {};
  ngOnInit() { if (this.exercise) this.form.patchValue(this.exercise); }
  nonBlank(control: AbstractControl) { return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null; }
  validation(key: keyof ExerciseInput): string {
    const control = this.form.controls[key];
    if (control.touched && control.hasError('required')) return 'Ce champ est obligatoire.';
    if (control.touched && control.hasError('maxlength')) return `Maximum ${control.getError('maxlength').requiredLength} caractères.`;
    return this.serverErrors[key] ?? '';
  }
  submit() {
    if (this.busy) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy = true; this.error = ''; this.serverErrors = {};
    const input = this.form.getRawValue();
    input.name = input.name.trim(); input.description = input.description.trim(); input.instructions = input.instructions.trim();
    this.api.save(input, this.exercise?.id).pipe(finalize(() => this.busy = false)).subscribe({
      next: response => this.saved.emit(response.message),
      error: error => {
        this.error = apiError(error);
        if (error instanceof HttpErrorResponse) this.serverErrors = error.error?.data?.errors ?? {};
      }
    });
  }
}

