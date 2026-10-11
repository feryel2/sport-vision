import { Component, DestroyRef, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TrainingAiService } from '../../services/training-ai.service';
import { apiError } from '../../services/exercise.service';
import { AiOptions, AiPreferences, AiPreview } from '../../models/training-ai';
@Component({selector:'app-ai-program',standalone:true,imports:[CommonModule,RouterLink,ReactiveFormsModule],templateUrl:'./ai-program.component.html',styleUrls:['./programs.css','./ai-program.css']})
export class AiProgramComponent {
  private readonly api=inject(TrainingAiService);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);
  options?:AiOptions;preview?:AiPreview;submitted?:AiPreferences;
  private confirmationKey?:string;
  loading=false;busy=false;error='';optionsError='';
  readonly form=new FormGroup({
    objective:new FormControl('GENERAL_FITNESS',{nonNullable:true,validators:[Validators.required]}),
    level:new FormControl('BEGINNER',{nonNullable:true,validators:[Validators.required]}),
    sessionCount:new FormControl(3,{nonNullable:true,validators:[Validators.required,Validators.min(1),Validators.max(5),Validators.pattern(/^[1-5]$/)]}),
    equipment:new FormControl<string[]>(['NONE'],{nonNullable:true,validators:[Validators.required]})
  });
  constructor(){this.loadOptions();}
  loadOptions(){if(this.loading)return;this.loading=true;this.optionsError='';
    this.api.options().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading=false)).subscribe({next:o=>this.options=o,error:e=>this.optionsError=apiError(e)});
  }
  toggleEquipment(value:string,checked:boolean){
    if(value==='NONE'){this.form.controls.equipment.setValue(['NONE']);return;}
    let selected=this.form.controls.equipment.value.filter(v=>v!=='NONE'&&v!==value);
    if(checked)selected.push(value);
    this.form.controls.equipment.setValue(selected.length?selected:['NONE']);this.form.controls.equipment.markAsDirty();
  }
  checked(event:Event){return (event.target as HTMLInputElement).checked;}
  generate(){
    if(this.busy)return;this.form.markAllAsTouched();if(this.form.invalid)return;
    this.error='';this.busy=true;this.submitted=this.form.getRawValue();
    this.api.generate(this.submitted).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:p=>{this.preview=p;this.confirmationKey=crypto.randomUUID();},error:e=>this.error=this.aiError(e)});
  }
  back(){if(this.busy)return;this.preview=undefined;this.confirmationKey=undefined;this.error='';}
  save(){
    if(this.busy||!this.preview||!this.submitted||!this.confirmationKey)return;this.busy=true;this.error='';
    this.api.save(this.submitted,this.preview.proposal,this.confirmationKey).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:p=>this.router.navigate(['/entrainement/programmes',p.id],{state:{notice:'Programme proposé par l’IA enregistré.'}}),error:e=>this.error=this.aiError(e)});
  }
  private aiError(error:unknown):string{
    // Les erreurs IA contrôlées (503, 502, 504…) doivent garder leur explication.
    if(error instanceof HttpErrorResponse&&error.error?.success===false&&typeof error.error?.message==='string'&&error.error?.data?.code)
      return error.error.message;
    return apiError(error);
  }
  exerciseName(id:number){return this.preview?.catalogue.find(e=>e.id===id)?.name??'Exercice indisponible';}
  equipmentLabel(id:number){const code=this.preview?.catalogue.find(e=>e.id===id)?.equipment;return this.options?.equipment.find(e=>e.value===code)?.label??'';}
}

