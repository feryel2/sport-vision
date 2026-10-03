import { Category } from './category.model';
import { Brand } from './brand.model';
import { Supplier } from './supplier.model';

export interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  stock: number;
  category: Category;
  brand?: Brand;
  supplier?: Supplier;
  sportGoal?: string;
  imageUrl: string;
  featured: boolean;
  rating: number;
  reviewsCount: number;
  specifications?: string;
  createdAt?: string;
}

export interface ProductFormData {
  name: string;
  slug?: string;
  description: string;
  price: number;
  stock: number;
  categoryId: number;
  brandId?: number;
  supplierId?: number;
  sportGoal?: string;
  imageUrl: string;
  featured: boolean;
  rating?: number;
  reviewsCount?: number;
  specifications?: string;
}
