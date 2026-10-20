import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, catchError, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { TRAINING_API_BASE_URL } from '../config/training-api.config';
import { ApiResponse, PageResult } from '../models/exercise';
import { Workout, WorkoutSummary, SetPerformance } from '../models/workout';
import { apiError } from './exercise.service';
@Injectable({providedIn:'root'})
export class WorkoutService {
  private readonly http=inject(HttpClient);
  private readonly url=inject(TRAINING_API_BASE_URL)+'/workouts';
  private readonly refresh=new BehaviorSubject<void>(undefined);
  readonly active$=this.refresh.pipe(switchMap(()=>this.http.get<ApiResponse<WorkoutSummary|null>>(this.url+'/current').pipe(map(r=>({workout:this.unwrap(r),error:''})),catchError(e=>of({workout:null,error:e.status===403?'':apiError(e)})))),shareReplay({bufferSize:1,refCount:true}));
  refreshCurrent(){this.refresh.next();}
  private unwrap<T>(r:ApiResponse<T>):T {if(!r.success)throw new Error(r.message);return r.data;}
  start(programId:number,revision:number,sessionId:number){return this.http.post<ApiResponse<Workout>>(this.url,{programId,revision,sessionId}).pipe(map(r=>this.unwrap(r)),tap(()=>this.refreshCurrent()));}
  get(id:number){return this.http.get<ApiResponse<Workout>>(this.url+'/'+id).pipe(map(r=>this.unwrap(r)));}
  history(status:string,page:number){return this.http.get<ApiResponse<PageResult<WorkoutSummary>>>(this.url,{params:{...(status?{status}:{}),page,size:10}}).pipe(map(r=>this.unwrap(r)));}
  saveSet(id:number,setId:number,input:SetPerformance){return this.http.put<ApiResponse<Workout>>(`${this.url}/${id}/sets/${setId}`,input).pipe(map(r=>this.unwrap(r)));}
  skip(id:number,setId:number){return this.http.patch<ApiResponse<Workout>>(`${this.url}/${id}/sets/${setId}/skip`,{}).pipe(map(r=>this.unwrap(r)));}
  complete(id:number,confirmSkipPending:boolean,comment:string){return this.http.post<ApiResponse<Workout>>(`${this.url}/${id}/complete`,{confirmSkipPending,comment}).pipe(map(r=>this.unwrap(r)),tap(()=>this.refreshCurrent()));}
  abandon(id:number,comment:string){return this.http.post<ApiResponse<Workout>>(`${this.url}/${id}/abandon`,{comment}).pipe(map(r=>this.unwrap(r)),tap(()=>this.refreshCurrent()));}
}
