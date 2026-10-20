import { Option } from './exercise';
import { Prescription } from './program';
export type AlternativeMotive='EQUIPMENT_UNAVAILABLE'|'PERSONAL_PREFERENCE';
export interface AlternativeRequest {expectedRevision:number;prescriptionId:number;motive:AlternativeMotive;level:string;equipment:string[];}
export interface AlternativePreview {request:AlternativeRequest;current:Prescription;alternative:AiCatalogExercise;reason:string;}
export interface AiPreferences {objective:string;level:string;sessionCount:number;equipment:string[];}
export interface AiChoice {exerciseId:number;measurementType:'REPETITIONS'|'DURATION';sets:number;repetitions:number|null;durationSeconds:number|null;restSeconds:number;}
export interface AiProposal {name:string;description:string;justification:string;sessions:{name:string;exercises:AiChoice[]}[];}
export interface AiCatalogExercise {id:number;name:string;muscleGroup:string;equipment:string;measurementType:string;}
export interface AiPreview {proposal:AiProposal;catalogue:AiCatalogExercise[];}
export interface AiOptions {objectives:Option[];levels:Option[];equipment:Option[];}
