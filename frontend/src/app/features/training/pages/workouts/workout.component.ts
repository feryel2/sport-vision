import { Component, DestroyRef, ElementRef, HostListener, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, Subject, finalize, interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WorkoutService } from '../../services/workout.service';
import { apiError } from '../../services/exercise.service';
import { Workout, WorkoutExercise, WorkoutSet, workoutLabels, setLabels, elapsedLabel } from '../../models/workout';
type SetForm=FormGroup<{performance:FormControl<number|null>;load:FormControl<number|null>}>;
@Component({selector:'app-workout',standalone:true,imports:[CommonModule,RouterLink,ReactiveFormsModule],templateUrl:'./workout.component.html',styleUrls:['../programs/programs.css','./workouts.css']})
export class WorkoutComponent {
  private readonly api=inject(WorkoutService);private readonly route=inject(ActivatedRoute);private readonly destroy=inject(DestroyRef);
  readonly labels=workoutLabels;readonly setLabels=setLabels;readonly elapsedLabel=elapsedLabel;
  workout?:Workout;loading=false;busy=false;savingId:number|null=null;error='';message='';rowErrors:Record<number,string>={};
  forms:Record<number,SetForm>={};readonly comment=new FormControl('',{nonNullable:true,validators:[Validators.maxLength(2000)]});
  modal:'leave'|'complete'|'abandon'|null=null;private leaveDecision?:Subject<boolean>;private priorFocus?:HTMLElement;
  @ViewChild('cancelButton') set cancelButton(el:ElementRef<HTMLButtonElement>|undefined){if(el)el.nativeElement.focus();}
  private syncedAt=Date.now();elapsed=0;
  get active(){return this.workout?.status==='IN_PROGRESS';}
  constructor(){
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(()=>this.load());
    interval(1000).pipe(takeUntilDestroyed(this.destroy)).subscribe(()=>{if(this.workout)this.elapsed=this.workout.elapsedSeconds+(this.active?Math.floor((Date.now()-this.syncedAt)/1000):0);});
    this.destroy.onDestroy(()=>{this.leaveDecision?.next(false);this.leaveDecision?.complete();});
  }
  load(){if(this.busy)return;this.loading=true;this.error='';
    this.api.get(Number(this.route.snapshot.paramMap.get('id'))).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading=false)).subscribe({next:w=>{
      this.apply(w);this.forms={};this.rowErrors={};
      for(const ex of w.exercises)for(const s of ex.sets){this.forms[s.id]=new FormGroup({performance:new FormControl(s.repetitions??s.durationSeconds,[Validators.required,Validators.min(1),Validators.max(ex.measurementType==='REPETITIONS'?10000:86400),Validators.pattern(/^\d+$/)]),load:new FormControl(s.loadKg,[Validators.min(0),Validators.max(10000),Validators.pattern(/^\d+(\.\d{1,2})?$/)])});}
      this.comment.reset(w.comment??'');
    },error:e=>this.error=apiError(e)});
  }
  private apply(w:Workout){this.workout=w;this.elapsed=w.elapsedSeconds;this.syncedAt=Date.now();}
  private dirtySets(){return Object.values(this.forms).some(f=>f.dirty);}
  private dirty(){return this.active&&(this.dirtySets()||this.comment.dirty);}
  save(ex:WorkoutExercise,s:WorkoutSet){if(this.busy||!this.active)return;const form=this.forms[s.id];form.markAllAsTouched();if(form.invalid)return;
    const values=form.getRawValue();this.mutateSet(s.id,this.api.saveSet(this.workout!.id,s.id,{repetitions:ex.measurementType==='REPETITIONS'?values.performance:null,durationSeconds:ex.measurementType==='DURATION'?values.performance:null,loadKg:values.load}));
  }
  skip(s:WorkoutSet){if(this.busy||!this.active)return;this.mutateSet(s.id,this.api.skip(this.workout!.id,s.id));}
  private mutateSet(id:number,request:Observable<Workout>){this.busy=true;this.savingId=id;this.rowErrors[id]='';this.message='';this.error='';
    request.pipe(takeUntilDestroyed(this.destroy),finalize(()=>{this.busy=false;this.savingId=null;})).subscribe({next:w=>{this.apply(w);const saved=w.exercises.flatMap(e=>e.sets).find(s=>s.id===id)!;this.forms[id].reset({performance:saved.repetitions??saved.durationSeconds,load:saved.loadKg});this.message='Série enregistrée.';},error:e=>this.rowErrors[id]=apiError(e)});
  }
  finish(confirm=false){if(this.busy||!this.active)return;this.error='';this.message='';
    if(this.dirtySets()){this.error='Enregistrez ou ignorez les séries modifiées avant de terminer.';return;}
    this.comment.markAsTouched();if(this.comment.invalid)return;
    this.busy=true;this.api.complete(this.workout!.id,confirm,this.comment.value).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:w=>{this.apply(w);this.closeModal();this.comment.markAsPristine();this.message='Séance terminée et enregistrée.';},error:e=>{if(e.error?.data?.code==='PENDING_CONFIRMATION_REQUIRED')this.openModal('complete');else{this.error=apiError(e);this.closeModal();}}});
  }
  abandon(){if(this.busy||!this.active)return;this.comment.markAsTouched();if(this.comment.invalid){this.closeModal();return;}
    this.busy=true;this.error='';this.api.abandon(this.workout!.id,this.comment.value).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:w=>{this.apply(w);this.closeModal();this.comment.markAsPristine();this.message='Séance abandonnée. Les séries enregistrées sont conservées.';},error:e=>{this.error=apiError(e);this.closeModal();}});
  }
  openModal(kind:'leave'|'complete'|'abandon'){this.priorFocus=document.activeElement as HTMLElement;this.modal=kind;}
  closeModal(){this.modal=null;this.priorFocus?.focus();}
  cancel(){if(this.busy)return;if(this.modal==='leave'){this.leaveDecision?.next(false);this.leaveDecision?.complete();this.leaveDecision=undefined;}this.closeModal();}
  confirm(){if(this.busy)return;if(this.modal==='complete')this.finish(true);else if(this.modal==='abandon')this.abandon();else if(this.modal==='leave'){const decision=this.leaveDecision;this.leaveDecision=undefined;this.closeModal();decision?.next(true);decision?.complete();}}
  canLeave():boolean|Observable<boolean>{if(this.busy){this.error='Enregistrement en cours, veuillez patienter.';return false;}if(!this.dirty())return true;this.leaveDecision=new Subject<boolean>();this.openModal('leave');return this.leaveDecision.asObservable();}
  @HostListener('window:beforeunload',['$event']) beforeUnload(event:BeforeUnloadEvent){if(this.dirty()||this.busy){event.preventDefault();event.returnValue='';}}
  modalKey(event:KeyboardEvent){if(event.key==='Escape'){event.preventDefault();this.cancel();}if(event.key==='Tab'){const buttons=Array.from((event.currentTarget as HTMLElement).querySelectorAll<HTMLButtonElement>('button:not(:disabled)'));if(!buttons.length){event.preventDefault();return;}const first=buttons[0],last=buttons[buttons.length-1];if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}}}
}
