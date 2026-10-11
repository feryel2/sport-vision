import { Component, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WorkoutService } from '../services/workout.service';
@Component({
  selector:'app-training-layout',standalone:true,
  imports:[CommonModule,RouterLink,RouterLinkActive,RouterOutlet],
  templateUrl:'./training-layout.component.html',styleUrls:['./training-layout.component.css']
})
export class TrainingLayoutComponent {
  readonly workouts=inject(WorkoutService);
  constructor(){inject(Router).events.pipe(filter(e=>e instanceof NavigationEnd),takeUntilDestroyed(inject(DestroyRef))).subscribe(()=>this.workouts.refreshCurrent());}
}
