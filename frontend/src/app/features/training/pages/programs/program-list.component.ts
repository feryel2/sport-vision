import { Component, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, merge, map, tap, switchMap, timer, startWith, catchError, of, finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProgramService } from '../../services/program.service';
import { apiError } from '../../services/exercise.service';
import { ProgramSummary } from '../../models/program';
import { PageResult } from '../../models/exercise';
interface ListState { loading:boolean; error?:string; result?:PageResult<ProgramSummary>; }
@Component({selector:'app-program-list',standalone:true,imports:[CommonModule,ReactiveFormsModule,RouterLink],templateUrl:'./program-list.component.html',styleUrls:['./programs.css']})
export class ProgramListComponent {
  private readonly api=inject(ProgramService);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);
  readonly filters=inject(FormBuilder).nonNullable.group({search:'',status:'ACTIVE'});
  readonly refresh=new Subject<void>();page=0;busy=false;error='';message='';confirmArchive:ProgramSummary|null=null;
  readonly state$=merge(this.filters.valueChanges.pipe(tap(()=>this.page=0),map(()=>true)),this.refresh.pipe(map(()=>false))).pipe(
    startWith(false),switchMap(delay=>timer(delay?300:0).pipe(switchMap(()=>{const f=this.filters.getRawValue();return this.api.list(f.search,f.status,this.page);}),
    map(result=>({loading:false,result} as ListState)),catchError(e=>of({loading:false,error:apiError(e)} as ListState)),startWith({loading:true} as ListState))));
  go(page:number){this.page=page;this.refresh.next();}
  duplicate(p:ProgramSummary){if(this.busy)return;this.busy=true;this.error='';this.message='';
    this.api.duplicate(p.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:copy=>this.router.navigate(['/entrainement/programmes',copy.id],{state:{notice:'Programme dupliqué.'}}),error:e=>this.error=apiError(e)});
  }
  changeStatus(p:ProgramSummary){if(this.busy)return;this.busy=true;this.error='';this.message='';
    this.api.status(p.id,p.currentRevision,p.archived).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:()=>{this.confirmArchive=null;this.message=p.archived?'Programme réactivé.':'Programme archivé.';this.go(0);},error:e=>this.error=apiError(e)});
  }
}
