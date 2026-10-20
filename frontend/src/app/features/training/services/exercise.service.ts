import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { map, shareReplay } from 'rxjs';
import { TRAINING_API_BASE_URL } from '../config/training-api.config';
import { ApiResponse, Exercise, ExerciseInput, ExerciseOptions, PageResult } from '../models/exercise';

@Injectable({ providedIn: 'root' })
export class ExerciseService {
  private readonly http = inject(HttpClient);
  private readonly url = `${inject(TRAINING_API_BASE_URL)}/exercises`;
  readonly options$ = this.http.get<ApiResponse<ExerciseOptions>>(`${this.url}/options`).pipe(
    map(response => this.unwrap(response)), shareReplay({ bufferSize: 1, refCount: true }));
  private unwrap<T>(response: ApiResponse<T>): T {
    if (!response.success) throw new Error(response.message);
    return response.data;
  }
  list(query: Record<string, string | number>) {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => { if (value !== '') params = params.set(key, value); });
    return this.http.get<ApiResponse<PageResult<Exercise>>>(this.url, { params }).pipe(map(r => this.unwrap(r)));
  }
  get(id: string) { return this.http.get<ApiResponse<Exercise>>(`${this.url}/${encodeURIComponent(id)}`).pipe(map(r => this.unwrap(r))); }
  save(input: ExerciseInput, id?: number) {
    const request = id === undefined
      ? this.http.post<ApiResponse<Exercise>>(this.url, input)
      : this.http.put<ApiResponse<Exercise>>(`${this.url}/${id}`, input);
    return request.pipe(map(r => { this.unwrap(r); return r; }));
  }
  setArchived(exercise: Exercise) {
    return this.http.patch<ApiResponse<Exercise>>(`${this.url}/${exercise.id}/${exercise.archived ? 'restore' : 'archive'}`, {}).pipe(
      map(r => { this.unwrap(r); return r; }));
  }
}
export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if ([0, 502, 503, 504].includes(error.status)) return 'Connexion au serveur impossible. Vérifiez que le backend est démarré, puis réessayez.';
    if (error.error?.success === false && typeof error.error?.message === 'string') return error.error.message;
  }
  return 'Impossible de terminer la demande. Réessayez.';
}

