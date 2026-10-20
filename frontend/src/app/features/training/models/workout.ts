export type WorkoutStatus='IN_PROGRESS'|'COMPLETED'|'ABANDONED';
export type SetStatus='PENDING'|'DONE'|'SKIPPED';
export interface WorkoutSet {id:number;position:number;status:SetStatus;repetitions:number|null;durationSeconds:number|null;loadKg:number|null;}
export interface WorkoutExercise {id:number;sourceExerciseId:number;sourcePrescriptionId:number;position:number;name:string;description:string;instructions:string;equipment:string;equipmentLabel:string;measurementType:'REPETITIONS'|'DURATION';measurementLabel:string;plannedSets:number;targetRepetitions:number|null;targetDurationSeconds:number|null;restSeconds:number;targetLoadKg:number|null;sets:WorkoutSet[];}
export interface WorkoutSummary {id:number;programName:string;sessionName:string;programRevision:number;status:WorkoutStatus;startedAt:string;endedAt:string|null;elapsedSeconds:number;totalSets:number;doneSets:number;skippedSets:number;}
export interface Workout extends WorkoutSummary {programId:number;programVersionId:number;sourceSessionId:number;programDescription:string;programObjective:string;sessionPosition:number;comment:string|null;pendingSets:number;exercises:WorkoutExercise[];}
export interface SetPerformance {repetitions:number|null;durationSeconds:number|null;loadKg:number|null;}
export const workoutLabels:Record<WorkoutStatus,string>={IN_PROGRESS:'En cours',COMPLETED:'Terminée',ABANDONED:'Abandonnée'};
export const setLabels:Record<SetStatus,string>={PENDING:'En attente',DONE:'Réalisée',SKIPPED:'Ignorée'};
export function elapsedLabel(seconds:number):string {const s=Math.max(0,Math.floor(seconds));return (s>=3600?Math.floor(s/3600)+' h ':'')+Math.floor(s%3600/60)+' min '+s%60+' s';}
