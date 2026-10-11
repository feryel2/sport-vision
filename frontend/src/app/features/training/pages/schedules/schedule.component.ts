import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ScheduleService } from '../../services/schedule.service';
import { ProgramService } from '../../services/program.service';
import { Schedule } from '../../models/schedule';
import { ProgramSummary, TrainingProgram } from '../../models/program';
import { PageResult } from '../../models/exercise';
@Component({standalone:true,selector:'sv-schedule',imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./schedule.component.html',styleUrls:['../programs/programs.css','./schedule.component.css']})
export class ScheduleComponent implements OnInit {
  private api=inject(ScheduleService);private programs=inject(ProgramService);private destroy=inject(DestroyRef);private route=inject(ActivatedRoute);private router=inject(Router);
  from=this.today();to=this.plusDays(this.from,30);page=0;result?:PageResult<Schedule>;groups:{date:string;items:Schedule[]}[]=[];
  search='';programPage=0;programList?:PageResult<ProgramSummary>;selected?:TrainingProgram;sessionId:number|null=null;scheduledLocal='';
  editing?:Schedule;editDate='';cancelling?:Schedule;busy=false;loading=false;programLoading=false;error='';message='';activeWorkoutId:number|null=null;
  ngOnInit(){this.load();this.searchPrograms();const id=Number(this.route.snapshot.queryParamMap.get('programId'));if(id>0)this.choose(id);}
  private today(){const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Tunis',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());return ['year','month','day'].map(t=>parts.find(p=>p.type===t)!.value).join('-');}
  private plusDays(day:string,count:number){const d=new Date(day+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+count);return d.toISOString().slice(0,10);}
  dateLabel(day:string){return day.split('-').reverse().join('/');}
  label(status:Schedule['status']){return {PLANNED:'Planifiée',IN_PROGRESS:'En cours',COMPLETED:'Effectuée',CANCELLED:'Annulée',ABANDONED:'Abandonnée'}[status];}
  load(page=0){if(this.loading)return;this.page=page;this.loading=true;this.error='';this.result=undefined;this.groups=[];this.editing=undefined;this.cancelling=undefined;
    this.api.list(this.from,this.to,page).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading=false)).subscribe({next:r=>{this.result=r;const map=new Map<string,Schedule[]>();for(const s of r.items){const key=s.scheduledLocal.slice(0,10);map.set(key,[...(map.get(key)||[]),s]);}this.groups=Array.from(map,([date,items])=>({date,items}));},error:e=>this.failure(e)});
  }
  searchPrograms(page=0){this.programPage=page;this.programLoading=true;
    this.programs.list(this.search,'ACTIVE',page).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.programLoading=false)).subscribe({next:r=>this.programList=r,error:e=>this.failure(e)});
  }
  choose(id:number){this.programLoading=true;this.selected=undefined;this.sessionId=null;
    this.programs.get(id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.programLoading=false)).subscribe({next:p=>{
      if(p.archived){this.error='Ce programme est archivé. Choisissez un programme actif.';return;}this.selected=p;
      const requested=Number(this.route.snapshot.queryParamMap.get('sessionId'));this.sessionId=p.version.sessions.find(s=>s.id===requested)?.id??p.version.sessions[0]?.id??null;
    },error:e=>this.failure(e)});
  }
  create(){if(!this.selected||!this.sessionId||!this.scheduledLocal||this.busy)return;this.busy=true;this.error='';this.message='';
    this.api.create(this.selected.id,this.selected.currentRevision,this.sessionId,this.scheduledLocal).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:s=>{this.message='Séance planifiée.';this.from=s.scheduledLocal.slice(0,10);this.to=this.plusDays(this.from,30);this.load();},error:e=>this.failure(e)});
  }
  edit(s:Schedule){this.editing=s;this.editDate=s.scheduledLocal.slice(0,16);this.cancelling=undefined;}
  save(){if(!this.editing||!this.editDate||this.busy)return;this.busy=true;this.error='';
    this.api.update(this.editing,this.editDate).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:s=>{this.message='Date modifiée.';this.from=s.scheduledLocal.slice(0,10);this.to=this.plusDays(this.from,30);this.load();},error:e=>this.failure(e)});
  }
  cancel(s:Schedule){if(this.busy)return;this.busy=true;this.error='';this.api.cancel(s).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:()=>{this.message='Planification annulée.';this.load(this.page);},error:e=>this.failure(e)});}
  start(s:Schedule){if(this.busy)return;this.busy=true;this.error='';this.api.start(s).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:w=>{void this.router.navigate(['/entrainement/seances',w.id]);},error:e=>this.failure(e)});}
  private failure(e:HttpErrorResponse){this.error=e.error?.message||(e.status===0?'Connexion impossible. Réessayez.':'La planification est indisponible.');if(e.status===409)this.error+=' Rechargez la liste et, si nécessaire, sélectionnez de nouveau le programme.';this.activeWorkoutId=e.error?.data?.workoutId??null;}
}
