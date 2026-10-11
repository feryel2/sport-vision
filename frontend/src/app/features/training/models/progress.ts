export interface ProgressPeriod {days:number;timeZone:string;firstDay:string;lastDay:string;fromInclusive:string;toExclusive:string;}
export interface ProgressPoint {workoutId:number;endedAt:string;date:string;sessionName:string;historicalNames:string[];doneSets:number;totalRepetitions:number|null;effortSeconds:number|null;maxLoadKg:number|null;}
export interface ProgressExercise {exerciseId:number;measurementType:'REPETITIONS'|'DURATION';name:string;points:ProgressPoint[];}
export interface ProgressDashboard {
  period:ProgressPeriod;indicators:{completedWorkouts:number;abandonedWorkouts:number;doneSets:number;elapsedSeconds:number};
  days:{date:string;completedWorkouts:number}[];exercises:ProgressExercise[];
}
