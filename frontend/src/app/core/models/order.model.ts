import { Product } from './product.model';

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  id: number;
  product: Product;
  quantity: number;
  unitPrice: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  shippingAddress: string;
  totalAmount: number;
  status: OrderStatus;
  paymentMethod: string;
  items: OrderItem[];
  createdAt: string;
}

export interface OrderCreateRequest {
  userId?: number;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  shippingAddress: string;
  paymentMethod?: string;
  items: {
    productId: number;
    quantity: number;
  }[];
}
