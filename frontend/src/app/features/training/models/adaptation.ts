export interface AdaptationValues { sets:number; repetitions:number|null; durationSeconds:number|null; restSeconds:number; targetLoadKg:number|null; }
export interface AdaptationReport {
  revision:number; completedWorkouts:number; eligible:boolean; message:string;
  items:{slot:number;session:string;exercise:string;measurementType:string;before:AdaptationValues;
    metrics:{workouts:number;done:number;skipped:number;actual:number;prescribed:number;difference:number;loadSamples:number;averageLoadKg:number|null;minimumLoadKg:number|null}}[];
}
export interface AdaptationPreview {proposalId:string|null;report:AdaptationReport;changes:{slot:number;session:string;exercise:string;reason:string;before:AdaptationValues;after:AdaptationValues}[];}
