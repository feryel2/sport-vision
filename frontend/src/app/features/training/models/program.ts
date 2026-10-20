export interface PrescriptionInput { existingPrescriptionId?: number | null; exerciseId: number; sets: number; repetitions: number | null; durationSeconds: number | null; restSeconds: number; targetLoadKg: number | null; }
export interface SessionInput { name: string; prescriptions: PrescriptionInput[]; }
export interface ProgramInput { name: string; description: string; objective: string; sessions: SessionInput[]; }
export interface Prescription extends PrescriptionInput { id: number; exerciseName: string; equipment: string; measurementType: string; exerciseArchived: boolean; position: number; }
export interface ProgramSession { id: number; name: string; position: number; prescriptions: Prescription[]; }
export interface ProgramVersion { id: number; revision: number; name: string; description: string; objective: string; archived: boolean; createdAt: string; sessions: ProgramSession[]; }
export interface ProgramSummary { id: number; name: string; objective: string; archived: boolean; currentRevision: number; sessionCount: number; updatedAt: string; }
export interface TrainingProgram { id: number; name: string; description: string; objective: string; archived: boolean; currentRevision: number; createdAt: string; updatedAt: string; version: ProgramVersion; }
export interface RevisionSummary { revision: number; name: string; archived: boolean; createdAt: string; }
