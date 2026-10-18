import { AfterViewInit, Component, DestroyRef, ElementRef, EventEmitter, Input, Output, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { TrainingAiService } from '../../services/training-ai.service';
import { apiError } from '../../services/exercise.service';
import { Prescription, TrainingProgram } from '../../models/program';
import { AiOptions, AlternativeMotive, AlternativePreview } from '../../models/training-ai';

@Component({selector:'app-alternative-dialog',standalone:true,imports:[CommonModule,ReactiveFormsModule],
  templateUrl:'./alternative-dialog.component.html',styleUrls:['./programs.css','./ai-program.css','./alternative-dialog.css']})
export class AlternativeDialogComponent implements AfterViewInit {
  @Input({required:true}) program!:TrainingProgram;
  @Input({required:true}) prescription!:Prescription;
  @Output() closed=new EventEmitter<void>();@Output() saved=new EventEmitter<TrainingProgram>();@Output() reload=new EventEmitter<void>();
  @ViewChild('dialog',{static:true}) dialog!:ElementRef<HTMLDialogElement>;
  private readonly api=inject(TrainingAiService);private readonly destroy=inject(DestroyRef);
  options?:AiOptions;preview?:AlternativePreview;busy=false;loading=false;error='';optionsError='';stale=false;
  readonly form=new FormGroup({motive:new FormControl<AlternativeMotive>('PERSONAL_PREFERENCE',{nonNullable:true,validators:[Validators.required]}),
    level:new FormControl('BEGINNER',{nonNullable:true,validators:[Validators.required]}),
    equipment:new FormControl<string[]>(['NONE'],{nonNullable:true,validators:[Validators.required]})});
  constructor(){this.loadOptions();}
  ngAfterViewInit(){this.dialog.nativeElement.showModal();}
  loadOptions(){if(this.loading)return;this.loading=true;this.optionsError='';
    this.api.options().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading=false)).subscribe({next:o=>this.options=o,error:e=>this.optionsError=apiError(e)});
  }
  toggle(value:string,event:Event){if(value==='NONE'){this.form.controls.equipment.setValue(['NONE']);return;}
    const values=this.form.controls.equipment.value.filter(v=>v!=='NONE'&&v!==value);
    if((event.target as HTMLInputElement).checked)values.push(value);
    this.form.controls.equipment.setValue(values.length?values:['NONE']);
  }
  generate(){if(this.busy||this.stale)return;this.form.markAllAsTouched();if(this.form.invalid)return;
    this.busy=true;this.error='';
    const request={...this.form.getRawValue(),expectedRevision:this.program.currentRevision,prescriptionId:this.prescription.id};
    this.api.alternative(this.program.id,request).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false))
      .subscribe({next:p=>this.preview=p,error:e=>this.handleError(e)});
  }
  confirm(){if(this.busy||this.stale||!this.preview)return;this.busy=true;this.error='';
    this.api.replace(this.program.id,this.preview.request,this.preview.alternative.id).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false))
      .subscribe({next:p=>{this.dialog.nativeElement.close();this.saved.emit(p);},error:e=>this.handleError(e)});
  }
  cancel(event?:Event){event?.preventDefault();if(this.busy)return;this.dialog.nativeElement.close();this.closed.emit();}
  private handleError(e:unknown){this.stale=e instanceof HttpErrorResponse&&e.status===409;
    this.error=e instanceof HttpErrorResponse&&e.error?.success===false&&e.error?.data?.code&&typeof e.error?.message==='string'?e.error.message:apiError(e);}
  equipmentLabel(code:string){return this.options?.equipment.find(e=>e.value===code)?.label??code;}
}
