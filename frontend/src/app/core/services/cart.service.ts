import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Product } from '../models/product.model';

export interface CartItem {
  product: Product;
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private itemsSubject = new BehaviorSubject<CartItem[]>(this.loadCart());
  public items$ = this.itemsSubject.asObservable();

  constructor() {}

  private loadCart(): CartItem[] {
    const raw = localStorage.getItem('sv_cart');
    if (raw) {
      try {
        return JSON.parse(raw);
      } catch (e) {
        return [];
      }
    }
    return [];
  }

  public get items(): CartItem[] {
    return this.itemsSubject.value;
  }

  public get totalCount(): number {
    return this.items.reduce((total, item) => total + item.quantity, 0);
  }

  public get subtotal(): number {
    return this.items.reduce((total, item) => total + (item.product.price * item.quantity), 0);
  }

  public get shippingCost(): number {
    return this.subtotal > 80 || this.subtotal === 0 ? 0 : 5.90;
  }

  public get total(): number {
    return this.subtotal + this.shippingCost;
  }

  public addToCart(product: Product, quantity: number = 1): void {
    const current = [...this.items];
    const index = current.findIndex(i => i.product.id === product.id);

    if (index > -1) {
      current[index].quantity += quantity;
    } else {
      current.push({ product, quantity });
    }

    this.saveCart(current);
  }

  public updateQuantity(productId: number, quantity: number): void {
    let current = [...this.items];
    if (quantity <= 0) {
      current = current.filter(i => i.product.id !== productId);
    } else {
      const index = current.findIndex(i => i.product.id === productId);
      if (index > -1) {
        current[index].quantity = quantity;
      }
    }
    this.saveCart(current);
  }

  public removeItem(productId: number): void {
    const current = this.items.filter(i => i.product.id !== productId);
    this.saveCart(current);
  }

  public clearCart(): void {
    this.saveCart([]);
  }

  private saveCart(items: CartItem[]): void {
    localStorage.setItem('sv_cart', JSON.stringify(items));
    this.itemsSubject.next(items);
  }
}
