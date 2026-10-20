import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, combineLatest, startWith, switchMap, forkJoin, map, catchError, of, finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProgramService } from '../../services/program.service';
import { ExerciseService, apiError } from '../../services/exercise.service';
import { TrainingProgram, RevisionSummary, Prescription } from '../../models/program';
import { AlternativeDialogComponent } from './alternative-dialog.component';
import { ExerciseOptions, PageResult, optionLabel } from '../../models/exercise';
import { WorkoutService } from '../../services/workout.service';
interface DetailState {loading:boolean;error?:string;program?:TrainingProgram;options?:ExerciseOptions;history?:PageResult<RevisionSummary>;historical?:boolean;}
@Component({selector:'app-program-detail',standalone:true,imports:[CommonModule,RouterLink,AlternativeDialogComponent],templateUrl:'./program-detail.component.html',styleUrls:['./programs.css']})
export class ProgramDetailComponent {
  alternativeTarget?:{program:TrainingProgram;prescription:Prescription};
  replaced(program:TrainingProgram){this.alternativeTarget=undefined;this.message=`Exercice remplacé. Révision ${program.currentRevision} enregistrée.`;this.historyPage=0;this.refresh.next();}
  reloadAlternative(){this.alternativeTarget=undefined;this.refresh.next();}
  private readonly api=inject(ProgramService);private readonly exercises=inject(ExerciseService);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);
  private readonly workouts=inject(WorkoutService);activeWorkoutId:number|null=null;readonly label=optionLabel;readonly refresh=new Subject<void>();historyPage=0;busy=false;error='';confirmArchive=false;
  message:string=this.router.getCurrentNavigation()?.extras.state?.['notice']??'';
  readonly state$=combineLatest([this.route.paramMap,this.route.queryParamMap,this.refresh.pipe(startWith(undefined))]).pipe(switchMap(([params,query])=>{
    const id=params.get('id')??'';const revision=query.has('revision')?Number(query.get('revision')):undefined;
    return forkJoin({program:this.api.get(id,revision),options:this.exercises.options$,history:this.api.revisions(Number(id),this.historyPage)}).pipe(map(r=>({...r,loading:false,historical:query.has('revision')} as DetailState)),catchError(e=>of({loading:false,error:apiError(e)} as DetailState)),startWith({loading:true} as DetailState));
  }));
  startSession(program:TrainingProgram,sessionId:number){if(this.busy)return;this.busy=true;this.error='';this.activeWorkoutId=null;
    this.workouts.start(program.id,program.version.revision,sessionId).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:w=>this.router.navigate(['/entrainement/seances',w.id]),error:e=>{this.error=apiError(e);this.activeWorkoutId=e.error?.data?.workoutId??null;}});
  }
  goHistory(page:number){this.historyPage=page;this.refresh.next();}
  duplicate(program:TrainingProgram){if(this.busy)return;this.busy=true;this.error='';this.message='';
    this.api.duplicate(program.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:p=>{this.message='Programme dupliqué.';this.historyPage=0;this.router.navigate(['/entrainement/programmes',p.id]);},error:e=>this.error=apiError(e)});
  }
  changeStatus(program:TrainingProgram){if(this.busy)return;this.busy=true;this.error='';this.message='';
    this.api.status(program.id,program.currentRevision,program.archived).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:()=>{this.confirmArchive=false;this.message=program.archived?'Programme réactivé.':'Programme archivé.';this.historyPage=0;this.refresh.next();},error:e=>this.error=apiError(e)});
  }
}


