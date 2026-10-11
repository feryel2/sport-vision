import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map } from 'rxjs';
import { TRAINING_API_BASE_URL } from '../config/training-api.config';
import { ApiResponse, PageResult } from '../models/exercise';
import { ProgramInput, ProgramSummary, RevisionSummary, TrainingProgram } from '../models/program';
@Injectable({providedIn:'root'})
export class ProgramService {
  private readonly http=inject(HttpClient);
  private readonly url=`${inject(TRAINING_API_BASE_URL)}/programs`;
  private unwrap<T>(r:ApiResponse<T>):T { if(!r.success) throw new Error(r.message); return r.data; }
  list(search:string,status:string,page:number) {
    return this.http.get<ApiResponse<PageResult<ProgramSummary>>>(this.url,{params:new HttpParams().set('search',search).set('status',status).set('page',page).set('size',9)}).pipe(map(r=>this.unwrap(r)));
  }
  get(id:string|number,revision?:number) { return this.http.get<ApiResponse<TrainingProgram>>(`${this.url}/${id}${revision ? '/revisions/'+revision : ''}`).pipe(map(r=>this.unwrap(r))); }
  revisions(id:number,page=0) { return this.http.get<ApiResponse<PageResult<RevisionSummary>>>(`${this.url}/${id}/revisions`,{params:{page,size:10}}).pipe(map(r=>this.unwrap(r))); }
  save(input:ProgramInput,id?:number,expectedRevision?:number) {
    const request=id===undefined ? this.http.post<ApiResponse<TrainingProgram>>(this.url,input) : this.http.put<ApiResponse<TrainingProgram>>(`${this.url}/${id}`,{expectedRevision,program:input});
    return request.pipe(map(r=>this.unwrap(r)));
  }
  duplicate(id:number) { return this.http.post<ApiResponse<TrainingProgram>>(`${this.url}/${id}/duplicate`,{}).pipe(map(r=>this.unwrap(r))); }
  status(id:number,revision:number,archived:boolean) { return this.http.patch<ApiResponse<TrainingProgram>>(`${this.url}/${id}/${archived?'restore':'archive'}`,{expectedRevision:revision}).pipe(map(r=>this.unwrap(r))); }
}
