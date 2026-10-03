import { Product } from './product.model';

export interface Favorite {
  id?: number;
  userEmail: string;
  product: Product;
  createdAt?: string;
}
