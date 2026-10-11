import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { TRAINING_API_BASE_URL } from '../config/training-api.config';
import { ApiResponse, PageResult } from '../models/exercise';
import { Schedule } from '../models/schedule';
import { Workout } from '../models/workout';
import { WorkoutService } from './workout.service';
@Injectable({providedIn:'root'})
export class ScheduleService {
  private http=inject(HttpClient);private url=inject(TRAINING_API_BASE_URL)+'/schedules';private workouts=inject(WorkoutService);
  private data<T>(r:ApiResponse<T>){if(!r.success)throw new Error(r.message);return r.data;}
  list(from:string,to:string,page:number){return this.http.get<ApiResponse<PageResult<Schedule>>>(this.url,{params:{from,to,page,size:20}}).pipe(map(r=>this.data(r)));}
  create(programId:number,expectedRevision:number,sessionId:number,scheduledLocal:string){return this.http.post<ApiResponse<Schedule>>(this.url,{programId,expectedRevision,sessionId,scheduledLocal}).pipe(map(r=>this.data(r)));}
  update(s:Schedule,scheduledLocal:string){return this.http.put<ApiResponse<Schedule>>(`${this.url}/${s.id}`,{expectedVersion:s.version,scheduledLocal}).pipe(map(r=>this.data(r)));}
  cancel(s:Schedule){return this.http.post<ApiResponse<Schedule>>(`${this.url}/${s.id}/cancel`,{expectedVersion:s.version}).pipe(map(r=>this.data(r)));}
  start(s:Schedule){return this.http.post<ApiResponse<Workout>>(`${this.url}/${s.id}/start`,{expectedVersion:s.version}).pipe(map(r=>this.data(r)),tap(()=>this.workouts.refreshCurrent()));}
}
