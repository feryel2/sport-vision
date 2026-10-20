export type Role = 'ROLE_USER' | 'ROLE_ADMIN';

export interface User {
  id: number;
  email: string;
  fullName: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  role: Role;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface UpdateProfilePayload {
  firstName: string;
  lastName: string;
  phone: string;
}
