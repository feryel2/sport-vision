import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, combineLatest, startWith, switchMap, catchError, map, of } from 'rxjs';
import { ProgressService } from '../../services/progress.service';
import { apiError } from '../../services/exercise.service';
import { ProgressDashboard, ProgressExercise } from '../../models/progress';
import { elapsedLabel } from '../../models/workout';
import { ChartPoint, ProgressChartComponent } from './progress-chart.component';
interface State {loading:boolean;error?:string;data?:ProgressDashboard;daily?:ChartPoint[];}
@Component({selector:'app-training-progress',standalone:true,imports:[CommonModule,RouterLink,ReactiveFormsModule,ProgressChartComponent],templateUrl:'./progress.component.html',styleUrls:['../programs/programs.css','./progress.css']})
export class ProgressComponent {
  private readonly api=inject(ProgressService);readonly days=new FormControl(30,{nonNullable:true});readonly refresh=new Subject<void>();
  readonly selection=new FormControl('',{nonNullable:true});readonly elapsedLabel=elapsedLabel;
  selected?:ProgressExercise;exerciseChart:ChartPoint[]=[];loadChart:ChartPoint[]=[];hasLoad=false;
  readonly state$=combineLatest([this.days.valueChanges.pipe(startWith(30)),this.refresh.pipe(startWith(undefined))]).pipe(switchMap(([days])=>{
    this.selected=undefined;
    return this.api.get(days).pipe(map(data=>{this.select(data.exercises);return {loading:false,data,daily:data.days.map(d=>({date:d.date,label:this.dayLabel(d.date),value:d.completedWorkouts}))} as State;}),catchError(e=>of({loading:false,error:apiError(e)} as State)),startWith({loading:true} as State));
  }));
  key(ex:ProgressExercise){return ex.exerciseId+':'+ex.measurementType;}
  dayLabel(date:string){return date.slice(8,10)+'/'+date.slice(5,7)+'/'+date.slice(0,4);}
  timeLabel(instant:string){return new Intl.DateTimeFormat('fr-FR',{timeZone:'Africa/Tunis',dateStyle:'short',timeStyle:'medium'}).format(new Date(instant));}
  select(exercises:ProgressExercise[],fromUser=false){
    this.selected=exercises.find(e=>this.key(e)===this.selection.value)??(fromUser?undefined:exercises[0]);
    this.selection.setValue(this.selected?this.key(this.selected):'',{emitEvent:false});
    const ex=this.selected;
    this.exerciseChart=ex?.points.map(p=>({date:p.endedAt,label:this.dayLabel(p.date),value:ex.measurementType==='REPETITIONS'?p.totalRepetitions:p.effortSeconds}))??[];
    this.loadChart=ex?.points.map(p=>({date:p.endedAt,label:this.dayLabel(p.date),value:p.maxLoadKg}))??[];
    this.hasLoad=this.loadChart.some(p=>p.value!==null);
  }
}
