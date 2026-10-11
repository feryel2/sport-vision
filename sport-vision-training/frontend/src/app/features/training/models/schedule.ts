export interface Schedule {
  id:number;version:number;programId:number;programRevision:number;sessionId:number;
  programName:string;sessionName:string;scheduledAt:string;scheduledLocal:string;timeZone:string;
  status:'PLANNED'|'IN_PROGRESS'|'COMPLETED'|'CANCELLED'|'ABANDONED';workoutId:number|null;
}
