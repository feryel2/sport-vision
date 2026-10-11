import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { TRAINING_API_BASE_URL } from '../config/training-api.config';
import { ApiResponse } from '../models/exercise';
import { TrainingProgram } from '../models/program';
import { AdaptationReport, AdaptationPreview } from '../models/adaptation';
@Injectable({providedIn:'root'})
export class AdaptationService {
  private http=inject(HttpClient);private base=inject(TRAINING_API_BASE_URL);
  private url(id:number){return `${this.base}/ai/programs/${id}/adaptation`;}
  report(id:number,expectedRevision:number){return this.http.get<ApiResponse<AdaptationReport>>(this.url(id),{params:{expectedRevision}}).pipe(map(r=>r.data));}
  propose(id:number,expectedRevision:number){return this.http.post<ApiResponse<AdaptationPreview>>(this.url(id),{expectedRevision}).pipe(map(r=>r.data));}
  confirm(id:number,expectedRevision:number,proposalId:string){return this.http.post<ApiResponse<TrainingProgram>>(this.url(id)+'/confirm',{expectedRevision,proposalId}).pipe(map(r=>r.data));}
}
