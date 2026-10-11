import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { TRAINING_API_BASE_URL } from '../config/training-api.config';
import { ApiResponse } from '../models/exercise';
import { TrainingProgram } from '../models/program';
import { AiPreferences, AiPreview, AiOptions, AiProposal, AlternativeRequest, AlternativePreview } from '../models/training-ai';
@Injectable({providedIn:'root'})
export class TrainingAiService {
  private readonly http=inject(HttpClient);private readonly url=inject(TRAINING_API_BASE_URL)+'/ai';
  private data<T>(response:ApiResponse<T>):T{if(!response.success)throw new Error(response.message);return response.data;}
  options(){return this.http.get<ApiResponse<AiOptions>>(this.url+'/options').pipe(map(r=>this.data(r)));}
  generate(preferences:AiPreferences){return this.http.post<ApiResponse<AiPreview>>(this.url+'/generate',preferences).pipe(map(r=>this.data(r)));}
  save(preferences:AiPreferences,proposal:AiProposal,idempotencyKey:string){return this.http.post<ApiResponse<TrainingProgram>>(this.url+'/programs',{preferences,proposal,idempotencyKey}).pipe(map(r=>this.data(r)));}
  alternative(id:number,request:AlternativeRequest){return this.http.post<ApiResponse<AlternativePreview>>(`${this.url}/programs/${id}/alternative`,request).pipe(map(r=>this.data(r)));}
  replace(id:number,request:AlternativeRequest,exerciseId:number){return this.http.post<ApiResponse<TrainingProgram>>(`${this.url}/programs/${id}/replacement`,{request,exerciseId}).pipe(map(r=>this.data(r)));}
}
