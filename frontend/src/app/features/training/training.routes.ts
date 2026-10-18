import { Routes } from '@angular/router';
import { unsavedProgramGuard } from './pages/programs/unsaved-program.guard';
import { TrainingLayoutComponent } from './layouts/training-layout.component';
export const TRAINING_ROUTES: Routes = [
  { path: '', component: TrainingLayoutComponent, data: { embedded: true }, children: [
    { path: '', pathMatch: 'full', title: 'Sport Vision • Entraînement',
      loadComponent: () => import('./pages/home/training-home.component').then(m => m.TrainingHomeComponent) },
    { path: 'seances/:id', title: 'Séance • Sport Vision', canDeactivate: [unsavedProgramGuard], loadComponent: () => import('./pages/workouts/workout.component').then(m => m.WorkoutComponent) },
    { path: 'planification', title: 'Planification • Sport Vision', loadComponent: () => import('./pages/schedules/schedule.component').then(m => m.ScheduleComponent) },
    { path: 'progression', title: 'Progression • Sport Vision', loadComponent: () => import('./pages/progress/progress.component').then(m => m.ProgressComponent) },
    { path: 'historique', title: 'Historique des séances • Sport Vision', loadComponent: () => import('./pages/workouts/workout-history.component').then(m => m.WorkoutHistoryComponent) },
    { path: 'programmes', title: 'Mes programmes • Sport Vision', loadComponent: () => import('./pages/programs/program-list.component').then(m => m.ProgramListComponent) },
    { path: 'programmes/generer', title: 'Générer un programme • Sport Vision', loadComponent: () => import('./pages/programs/ai-program.component').then(m => m.AiProgramComponent) },
    { path: 'programmes/nouveau', title: 'Créer un programme • Sport Vision', canDeactivate: [unsavedProgramGuard], loadComponent: () => import('./pages/programs/program-editor.component').then(m => m.ProgramEditorComponent) },
    { path: 'programmes/:id/modifier', title: 'Modifier le programme • Sport Vision', canDeactivate: [unsavedProgramGuard], loadComponent: () => import('./pages/programs/program-editor.component').then(m => m.ProgramEditorComponent) },
    { path: 'programmes/:id/adaptation', title: 'Bilan et adaptation IA • Sport Vision', loadComponent: () => import('./pages/programs/adaptation.component').then(m => m.AdaptationComponent) },
    { path: 'programmes/:id', title: 'Programme • Sport Vision', loadComponent: () => import('./pages/programs/program-detail.component').then(m => m.ProgramDetailComponent) },
    { path: 'exercices', title: 'Catalogue des exercices • Sport Vision',
      loadComponent: () => import('./pages/exercises/exercise-list.component').then(m => m.ExerciseListComponent) },
    { path: 'exercices/:id', title: 'Fiche exercice • Sport Vision',
      loadComponent: () => import('./pages/exercises/exercise-detail.component').then(m => m.ExerciseDetailComponent) }
  ] }
];
