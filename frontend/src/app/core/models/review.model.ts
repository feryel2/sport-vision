import { Product } from './product.model';

export interface Review {
  id?: number;
  product?: Product;
  productId?: number;
  productName?: string;
  authorName: string;
  authorEmail?: string;
  rating: number;
  comment: string;
  createdAt?: string;
}
