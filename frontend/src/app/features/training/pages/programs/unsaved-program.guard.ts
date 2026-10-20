import { CanDeactivateFn } from '@angular/router';
import { Observable } from 'rxjs';
export interface UnsavedProgram { canLeave():boolean|Observable<boolean>; }
export const unsavedProgramGuard:CanDeactivateFn<UnsavedProgram> = component=>component.canLeave();
