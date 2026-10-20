export interface ApiResponse<T> { success: boolean; message: string; data: T; }
export interface ExerciseInput {
  name: string; description: string; instructions: string;
  muscleGroup: string; equipment: string; difficulty: string; measurementType: string;
}
export interface Exercise extends ExerciseInput {
  id: number; archived: boolean; createdAt: string; updatedAt: string;
}
export interface PageResult<T> { items: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface Option { value: string; label: string; }
export interface ExerciseOptions { muscleGroups: Option[]; equipment: Option[]; difficulties: Option[]; measurementTypes: Option[]; }
export type OptionKey = keyof ExerciseOptions;
export function optionLabel(options: ExerciseOptions, key: OptionKey, value: string): string {
  return options[key].find(option => option.value === value)?.label ?? 'Non renseigné';
}
