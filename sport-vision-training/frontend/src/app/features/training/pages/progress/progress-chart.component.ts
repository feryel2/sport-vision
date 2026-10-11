import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
export interface ChartPoint {date:string;label:string;value:number|null;}
@Component({
  selector:'app-progress-chart',standalone:true,imports:[CommonModule],
  template: `
    <svg viewBox="0 0 800 280" role="img" [attr.aria-label]="title+'. '+unit+'. Valeurs détaillées dans le tableau associé.'">
      <title>{{title}}</title><desc>{{description}}</desc>
      <g *ngFor="let tick of ticks"><line x1="62" x2="766" [attr.y1]="tick.y" [attr.y2]="tick.y" class="grid"/><text x="52" [attr.y]="tick.y+5" text-anchor="end">{{tick.label}}</text></g>
      <text x="62" y="19">{{unit}}</text>
      <ng-container *ngIf="kind==='bars'">
        <rect *ngFor="let p of plotted" [attr.x]="p.x-barWidth/2" [attr.y]="p.y" [attr.width]="barWidth" [attr.height]="232-p.y" rx="2"><title>{{p.label}} : {{p.value}} {{unit}}</title></rect>
      </ng-container>
      <ng-container *ngIf="kind==='line'">
        <path *ngFor="let segment of segments" [attr.d]="segment" class="series"/>
        <circle *ngFor="let p of plotted" [attr.cx]="p.x" [attr.cy]="p.y" r="5"><title>{{p.label}} : {{p.value}} {{unit}}</title></circle>
      </ng-container>
      <line x1="62" x2="766" y1="232" y2="232" class="axis"/>
      <text *ngIf="points.length" x="62" y="260">{{points[0].label}}</text>
      <text *ngIf="points.length>1" x="766" y="260" text-anchor="end">{{points[points.length-1].label}}</text>
    </svg>`,
  styles:[`:host{display:block}svg{width:100%;height:auto;min-height:160px;overflow:visible}text{font-family:inherit;font-size:15px;fill:var(--sv-gray-600)}.grid{stroke:var(--sv-gray-200);stroke-dasharray:3 4}.axis{stroke:var(--sv-gray-600)}rect,circle{fill:var(--sv-primary)}.series{stroke:var(--sv-primary);stroke-width:2.5;fill:none}circle{stroke:white;stroke-width:2}@media(max-width:640px){text{font-size:19px}}`]
})
export class ProgressChartComponent implements OnChanges {
  @Input() points:ChartPoint[]=[];@Input() title='';@Input() unit='';@Input() kind:'bars'|'line'='line';
  plotted:{x:number;y:number;value:number;label:string}[]=[];segments:string[]=[];ticks:{y:number;label:string}[]=[];barWidth=10;description='';
  ngOnChanges(){
    // These calculations only scale SVG coordinates; all business totals come from the API.
    const values=this.points.flatMap(p=>p.value===null?[]:[p.value]);const max=Math.max(1,...values);
    const dates=this.points.map(p=>Date.parse(p.date));const low=Math.min(...dates),high=Math.max(...dates);
    const x=(i:number)=>this.kind==='bars'?62+(i+.5)*704/this.points.length:high===low?414:70+(dates[i]-low)/(high-low)*688;
    const y=(n:number)=>232-n/max*194;
    this.plotted=this.points.flatMap((p,i)=>p.value===null?[]:[{x:x(i),y:y(p.value),value:p.value,label:p.label}]);
    this.barWidth=Math.max(2,Math.min(38,704/Math.max(1,this.points.length)*.65));
    this.ticks=[0,max/2,max].map(n=>({y:y(n),label:new Intl.NumberFormat('fr-FR',{maximumFractionDigits:2}).format(n)}));
    this.segments=[];
    for(let i=1;i<this.points.length;i++){const a=this.points[i-1],b=this.points[i];if(a.value!==null&&b.value!==null)this.segments.push('M '+x(i-1)+' '+y(a.value)+' L '+x(i)+' '+y(b.value));}
    this.description=values.length===1?'Une seule valeur observée ; aucune tendance.':values.length===0?'Aucune valeur renseignée.':'Les valeurs absentes ne sont pas remplacées par zéro. Consulter le tableau pour les valeurs exactes.';
  }
}
