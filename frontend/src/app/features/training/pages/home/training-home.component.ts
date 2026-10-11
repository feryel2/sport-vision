import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-training-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './training-home.component.html',
  styleUrls: ['./training-home.component.css']
})
export class TrainingHomeComponent {}