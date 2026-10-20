import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, combineLatest, startWith, switchMap, map, catchError, of } from 'rxjs';
import { WorkoutService } from '../../services/workout.service';
import { apiError } from '../../services/exercise.service';
import { WorkoutSummary, workoutLabels, elapsedLabel } from '../../models/workout';
import { PageResult } from '../../models/exercise';
interface HistoryState {loading:boolean;error?:string;page?:PageResult<WorkoutSummary>;}
@Component({selector:'app-workout-history',standalone:true,imports:[CommonModule,RouterLink,ReactiveFormsModule],templateUrl:'./workout-history.component.html',styleUrls:['../programs/programs.css','./workouts.css']})
export class WorkoutHistoryComponent {
  private readonly api=inject(WorkoutService);readonly status=new FormControl('',{nonNullable:true});
  readonly labels=workoutLabels;readonly elapsedLabel=elapsedLabel;readonly refresh=new Subject<void>();page=0;
  readonly state$=combineLatest([this.status.valueChanges.pipe(startWith(''),map(status=>{this.page=0;return status;})),this.refresh.pipe(startWith(undefined))]).pipe(switchMap(([status])=>this.api.history(status,this.page).pipe(map(page=>({loading:false,page} as HistoryState)),catchError(e=>of({loading:false,error:apiError(e)} as HistoryState)),startWith({loading:true} as HistoryState))));
  go(page:number){this.page=page;this.refresh.next();}
}
