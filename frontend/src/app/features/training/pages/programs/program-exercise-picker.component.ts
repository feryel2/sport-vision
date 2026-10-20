import { Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, merge, map, tap, startWith, switchMap, timer, forkJoin, catchError, of } from 'rxjs';
import { ExerciseService, apiError } from '../../services/exercise.service';
import { Exercise, ExerciseOptions, PageResult, optionLabel } from '../../models/exercise';
interface PickerState {loading:boolean;error?:string;result?:PageResult<Exercise>;options?:ExerciseOptions;}
@Component({selector:'app-program-exercise-picker',standalone:true,imports:[CommonModule,ReactiveFormsModule],templateUrl:'./program-exercise-picker.component.html',styleUrls:['./programs.css']})
export class ProgramExercisePickerComponent {
  @Output() selected=new EventEmitter<Exercise>();@Output() closed=new EventEmitter<void>();
  readonly search=new FormControl('',{nonNullable:true});readonly refresh=new Subject<void>();page=0;readonly label=optionLabel;
  private readonly api=inject(ExerciseService);
  readonly state$=merge(this.search.valueChanges.pipe(tap(()=>this.page=0),map(()=>true)),this.refresh.pipe(map(()=>false))).pipe(startWith(false),switchMap(delay=>timer(delay?300:0).pipe(switchMap(()=>forkJoin({options:this.api.options$,result:this.api.list({search:this.search.value,status:'ACTIVE',page:this.page,size:6})})),map(r=>({...r,loading:false} as PickerState)),catchError(e=>of({loading:false,error:apiError(e)} as PickerState)),startWith({loading:true} as PickerState))));
  go(page:number){this.page=page;this.refresh.next();}
}
