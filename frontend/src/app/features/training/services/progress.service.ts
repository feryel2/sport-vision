import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { TRAINING_API_BASE_URL } from '../config/training-api.config';
import { ApiResponse } from '../models/exercise';
import { ProgressDashboard } from '../models/progress';
@Injectable({providedIn:'root'})
export class ProgressService {
  private readonly http=inject(HttpClient);private readonly url=inject(TRAINING_API_BASE_URL)+'/progress';
  get(days:number){return this.http.get<ApiResponse<ProgressDashboard>>(this.url,{params:{days}}).pipe(map(r=>{if(!r.success)throw new Error(r.message);return r.data;}));}
}
