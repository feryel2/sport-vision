import { Component, DestroyRef, ElementRef, HostListener, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, finalize, forkJoin, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProgramService } from '../../services/program.service';
import { ExerciseService, apiError } from '../../services/exercise.service';
import { Exercise, ExerciseOptions, optionLabel } from '../../models/exercise';
import { Prescription, ProgramInput, ProgramSession, TrainingProgram } from '../../models/program';
import { ProgramExercisePickerComponent } from './program-exercise-picker.component';

function nonBlank(c:AbstractControl){return typeof c.value==='string'&&!c.value.trim()?{required:true}:null;}
const integer=[Validators.pattern(/^\d+$/)];
@Component({selector:'app-program-editor',standalone:true,imports:[CommonModule,ReactiveFormsModule,RouterLink,ProgramExercisePickerComponent],templateUrl:'./program-editor.component.html',styleUrls:['./programs.css']})
export class ProgramEditorComponent implements OnInit {
  private readonly fb=inject(FormBuilder);private readonly api=inject(ProgramService);private readonly exercises=inject(ExerciseService);
  private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly destroy=inject(DestroyRef);
  readonly id=this.route.snapshot.paramMap.get('id');readonly label=optionLabel;
  program?:TrainingProgram;options?:ExerciseOptions;loading=true;loadError='';error='';conflict=false;saving=false;pickerSession:number|null=null;submitted=false;serverErrors:string[]=[];
  createPrescription(exercise:Exercise|Prescription){
    const existing='exerciseId' in exercise ? exercise : null;
    return this.fb.group({
      existingPrescriptionId:new FormControl<number|null>(existing?.id??null),
      exerciseId:this.fb.nonNullable.control(existing?.exerciseId??exercise.id),
      exerciseName:this.fb.nonNullable.control(existing?.exerciseName??('name' in exercise?exercise.name:'')),
      equipment:this.fb.nonNullable.control(exercise.equipment),measurementType:this.fb.nonNullable.control(exercise.measurementType),
      exerciseArchived:this.fb.nonNullable.control(existing?.exerciseArchived??false),
      sets:new FormControl<number|null>(existing?.sets??null,[Validators.required,Validators.min(1),Validators.max(100),...integer]),
      repetitions:new FormControl<number|null>(existing?.repetitions??null,exercise.measurementType==='REPETITIONS'?[Validators.required,Validators.min(1),Validators.max(10000),...integer]:[]),
      durationSeconds:new FormControl<number|null>(existing?.durationSeconds??null,exercise.measurementType==='DURATION'?[Validators.required,Validators.min(1),Validators.max(86400),...integer]:[]),
      restSeconds:new FormControl<number|null>(existing?.restSeconds??0,[Validators.required,Validators.min(0),Validators.max(86400),...integer]),
      targetLoadKg:new FormControl<number|null>(existing?.targetLoadKg??null,[Validators.min(0),Validators.max(10000),Validators.pattern(/^\d+(\.\d{1,2})?$/)])
    });
  }
  createSession(session?:ProgramSession){return this.fb.group({name:this.fb.nonNullable.control(session?.name??'',[Validators.required,nonBlank,Validators.maxLength(120)]),prescriptions:this.fb.array((session?.prescriptions??[]).map(p=>this.createPrescription(p)),[Validators.required,Validators.maxLength(30)])});}
  readonly form=this.fb.group({
    name:this.fb.nonNullable.control('',[Validators.required,nonBlank,Validators.maxLength(120)]),
    description:this.fb.nonNullable.control('',[Validators.required,nonBlank,Validators.maxLength(2000)]),
    objective:this.fb.nonNullable.control('',[Validators.required,nonBlank,Validators.maxLength(500)]),
    sessions:this.fb.array<ReturnType<ProgramEditorComponent['createSession']>>([],[Validators.required,Validators.maxLength(20)])
  });
  get sessions(){return this.form.controls.sessions;}
  ngOnInit(){this.load();}
  load(){
    this.loading=true;this.loadError='';
    forkJoin({options:this.exercises.options$,program:this.id?this.api.get(this.id):of(null)}).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading=false)).subscribe({
      next:({options,program})=>{this.options=options;this.program=program??undefined;this.sessions.clear();this.pickerSession=null;
        if(program){this.form.patchValue({name:program.version.name,description:program.version.description,objective:program.version.objective});program.version.sessions.forEach(s=>this.sessions.push(this.createSession(s)));}
        else this.sessions.push(this.createSession());
        this.form.markAsPristine();this.form.markAsUntouched();this.submitted=false;this.error='';this.conflict=false;this.serverErrors=[];
      },error:e=>this.loadError=apiError(e)
    });
  }
  addSession(){if(this.sessions.length>=20)return;this.sessions.push(this.createSession());this.form.markAsDirty();this.pickerSession=null;}
  removeSession(index:number){this.sessions.removeAt(index);this.form.markAsDirty();this.pickerSession=null;}
  move(array:FormArray,index:number,direction:number){const destination=index+direction;if(destination<0||destination>=array.length)return;const control=array.at(index);array.removeAt(index);array.insert(destination,control);this.form.markAsDirty();this.pickerSession=null;}
  addExercise(index:number,exercise:Exercise){const list=this.sessions.at(index).controls.prescriptions;if(list.length>=30)return;list.push(this.createPrescription(exercise));this.form.markAsDirty();this.pickerSession=null;}
  removeExercise(session:number,index:number){this.sessions.at(session).controls.prescriptions.removeAt(index);this.form.markAsDirty();}
  validation(c:AbstractControl){
    if(!(c.touched||this.submitted))return '';
    if(c.hasError('required'))return 'Ce champ est obligatoire.';
    if(c.hasError('min'))return `Minimum technique : ${c.getError('min').min}.`;
    if(c.hasError('max'))return `Maximum technique : ${c.getError('max').max}.`;
    if(c.hasError('maxlength'))return `Maximum : ${c.getError('maxlength').requiredLength} caractères.`;
    if(c.hasError('pattern'))return 'Valeur numérique invalide (entier, ou charge avec 2 décimales maximum).';return '';
  }
  input():ProgramInput{
    const value=this.form.getRawValue();return {name:value.name.trim(),description:value.description.trim(),objective:value.objective.trim(),sessions:value.sessions.map(s=>({name:s.name.trim(),prescriptions:s.prescriptions.map(p=>({existingPrescriptionId:p.existingPrescriptionId,exerciseId:p.exerciseId,sets:p.sets!,repetitions:p.measurementType==='REPETITIONS'?p.repetitions:null,durationSeconds:p.measurementType==='DURATION'?p.durationSeconds:null,restSeconds:p.restSeconds!,targetLoadKg:p.targetLoadKg}))}))};
  }
  save(){
    if(this.saving)return;this.submitted=true;this.form.markAllAsTouched();this.error='';this.serverErrors=[];
    if(this.form.invalid){this.error='Vérifiez les champs et ajoutez au moins un exercice dans chaque séance.';return;}
    this.saving=true;this.conflict=false;
    this.api.save(this.input(),this.program?.id,this.program?.currentRevision).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.saving=false)).subscribe({
      next:program=>{this.saving=false;this.form.markAsPristine();this.router.navigate(['/entrainement/programmes',program.id],{state:{notice:this.id?'Programme enregistré.':'Programme créé.'}});},
      error:e=>{this.error=apiError(e);this.conflict=e instanceof HttpErrorResponse&&e.status===409;this.serverErrors=Object.values(e.error?.data?.errors??{});}
    });
  }
  leavePrompt=false;reloadPrompt=false;private leaveDecision?:Subject<boolean>;
  @ViewChild('stayButton') set stayButton(button:ElementRef<HTMLButtonElement>|undefined){if(button)button.nativeElement.focus();}
  reloadCurrent(){this.reloadPrompt=true;}
  resolveReload(discard:boolean){this.reloadPrompt=false;if(discard)this.load();}
  canLeave():boolean|Observable<boolean>{
    if(this.saving)return false;if(!this.form.dirty)return true;
    if(!this.leaveDecision)this.leaveDecision=new Subject<boolean>();this.leavePrompt=true;return this.leaveDecision.asObservable();
  }
  resolveLeave(discard:boolean){this.leavePrompt=false;const decision=this.leaveDecision;this.leaveDecision=undefined;decision?.next(discard);decision?.complete();}
  dialogKeys(event:KeyboardEvent){
    if(event.key==='Escape'){event.preventDefault();this.leavePrompt?this.resolveLeave(false):this.resolveReload(false);}
    if(event.key==='Tab'){
      const buttons=Array.from((event.currentTarget as HTMLElement).querySelectorAll<HTMLButtonElement>('button'));
      const current=buttons.indexOf(document.activeElement as HTMLButtonElement);
      event.preventDefault();buttons[(current+(event.shiftKey?-1:1)+buttons.length)%buttons.length]?.focus();
    }
  }
  @HostListener('window:beforeunload',['$event']) beforeUnload(event:BeforeUnloadEvent){if(this.form.dirty||this.saving){event.preventDefault();event.returnValue='';}}
}

