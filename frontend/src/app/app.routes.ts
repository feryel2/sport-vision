import { Routes } from '@angular/router';
import { TrainingLayoutComponent } from './features/training/layouts/training-layout.component';
export const routes: Routes = [
  { path: 'entrainement', loadChildren: () => import('./features/training/training.routes').then(m => m.TRAINING_ROUTES) },
  { path: 'admin/entrainement', component: TrainingLayoutComponent, children: [
    { path: 'exercices', title: 'Administration des exercices • Sport Vision', data: { admin: true },
      loadComponent: () => import('./features/training/pages/exercises/exercise-list.component').then(m => m.ExerciseListComponent) },
    { path: '', pathMatch: 'full', redirectTo: 'exercices' }
  ] },
  { path: '', pathMatch: 'full', redirectTo: 'entrainement' },
  { path: '**', redirectTo: 'entrainement' }
];
