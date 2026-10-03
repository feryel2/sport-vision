export interface Food {
  id?: number;
  name: string;
  category: string;
  calories: number;
  proteins: number;
  carbs: number;
  fats: number;
  portion: string;
  benefits?: string;
}
