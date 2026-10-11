import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProgramService } from '../../services/program.service';
import { AdaptationService } from '../../services/adaptation.service';
import { AdaptationReport, AdaptationPreview, AdaptationValues } from '../../models/adaptation';
@Component({standalone:true,selector:'sv-adaptation',imports:[CommonModule,RouterLink],templateUrl:'./adaptation.component.html',styleUrls:['./programs.css']})
export class AdaptationComponent implements OnInit {
  private programs=inject(ProgramService);private api=inject(AdaptationService);private route=inject(ActivatedRoute);private router=inject(Router);private destroy=inject(DestroyRef);
  id=Number(this.route.snapshot.paramMap.get('id'));name='';busy=false;error='';conflict=false;report?:AdaptationReport;preview?:AdaptationPreview;
  ngOnInit(){this.load();}
  load(){this.busy=true;this.error='';this.conflict=false;this.preview=undefined;this.report=undefined;
    this.programs.get(this.id).pipe(switchMap(p=>{this.name=p.name;return this.api.report(this.id,p.currentRevision);}),takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false))
      .subscribe({next:r=>this.report=r,error:e=>this.failure(e)});
  }
  propose(){if(!this.report||this.busy||this.conflict)return;this.busy=true;this.error='';this.preview=undefined;
    this.api.propose(this.id,this.report.revision).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false)).subscribe({next:p=>this.preview=p,error:e=>this.failure(e)});
  }
  confirm(){if(!this.preview?.proposalId||!this.report||this.busy||this.conflict)return;this.busy=true;this.error='';
    this.api.confirm(this.id,this.report.revision,this.preview.proposalId).pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.busy=false))
      .subscribe({next:p=>{void this.router.navigate(['/entrainement/programmes',p.id]);},error:e=>this.failure(e)});
  }
  private failure(e:HttpErrorResponse){this.conflict=e.status===409;this.error=e.error?.message||(e.status===0?'Connexion impossible. Réessayez lorsque le serveur est accessible.':'Le bilan est indisponible. Réessayez.');}
  values(v:AdaptationValues){return `${v.sets} séries × ${v.repetitions!==null?v.repetitions+' répétitions':v.durationSeconds+' s'} · repos ${v.restSeconds} s · charge ${v.targetLoadKg===null?'non renseignée':v.targetLoadKg+' kg'}`;}
}
