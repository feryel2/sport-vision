export interface Coach {
  id?: number;
  fullName: string;
  specialty: string;
  sport: string;
  targetLevel?: string;
  availability?: string;
  rating?: number;
  hourlyRate?: number;
  bio?: string;
  imageUrl?: string;
  email?: string;
  phone?: string;
}
