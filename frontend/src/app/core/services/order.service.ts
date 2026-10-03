import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of } from 'rxjs';
import { Order, OrderCreateRequest, OrderStatus } from '../models/order.model';
import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private apiUrl = 'http://localhost:8080/api';

  private defaultOrders: Order[] = [
    {
      id: 1,
      orderNumber: 'SV-20261001-A94F12',
      customerName: 'Sophie Martin',
      customerEmail: 'client@sportvision.com',
      customerPhone: '+33 6 98 76 54 32',
      shippingAddress: '14 Avenue des Athlètes, 75012 Paris, France',
      totalAmount: 329.70,
      status: 'DELIVERED',
      paymentMethod: 'CARTE_BANCAIRE',
      createdAt: '2026-10-01T10:15:00',
      items: [
        {
          id: 1,
          quantity: 1,
          unitPrice: 189.90,
          product: {
            id: 1,
            name: 'Lunettes Sport Vision HyperTrack Pro',
            slug: 'sport-vision-hypertrack-pro',
            description: 'Lunettes de soleil de sport à champ visuel panoramique',
            price: 189.90,
            stock: 24,
            category: { id: 1, name: 'Optique & Lunettes Sport', slug: 'optique-sport', description: '' },
            imageUrl: 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?auto=format&fit=crop&w=800&q=80',
            featured: true,
            rating: 4.9,
            reviewsCount: 48
          }
        },
        {
          id: 2,
          quantity: 2,
          unitPrice: 69.90,
          product: {
            id: 7,
            name: 'Maillot de Compression IsoVent Aero Blue',
            slug: 'maillot-compression-isovent-aero-blue',
            description: 'Haut technique à coupe aérodynamique',
            price: 69.90,
            stock: 45,
            category: { id: 2, name: 'Textiles Techniques', slug: 'textiles-techniques', description: '' },
            imageUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=800&q=80',
            featured: true,
            rating: 4.8,
            reviewsCount: 56
          }
        }
      ]
    },
    {
      id: 2,
      orderNumber: 'SV-20261002-B71C09',
      customerName: 'Marc Vasseur',
      customerEmail: 'marc.vasseur@club-cycliste.fr',
      customerPhone: '+33 6 44 22 11 00',
      shippingAddress: '8 Boulevard des Pyrénées, 64000 Pau, France',
      totalAmount: 239.00,
      status: 'SHIPPED',
      paymentMethod: 'CARTE_BANCAIRE',
      createdAt: '2026-10-02T14:30:00',
      items: [
        {
          id: 3,
          quantity: 1,
          unitPrice: 239.00,
          product: {
            id: 4,
            name: 'Chaussures Running SwiftPulse Carbon X',
            slug: 'swiftpulse-carbon-x',
            description: 'Chaussures marathon',
            price: 239.00,
            stock: 32,
            category: { id: 3, name: 'Chaussures & Running', slug: 'chaussures-running', description: '' },
            imageUrl: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80',
            featured: true,
            rating: 4.9,
            reviewsCount: 87
          }
        }
      ]
    },
    {
      id: 3,
      orderNumber: 'SV-20261003-C33E81',
      customerName: 'Camille Dupont',
      customerEmail: 'camille.dupont@orange.fr',
      customerPhone: '+33 7 88 99 00 11',
      shippingAddress: '25 Rue de la République, 69002 Lyon, France',
      totalAmount: 499.00,
      status: 'CONFIRMED',
      paymentMethod: 'PAYPAL',
      createdAt: '2026-10-03T09:45:00',
      items: [
        {
          id: 4,
          quantity: 1,
          unitPrice: 499.00,
          product: {
            id: 9,
            name: 'Montre GPS Connectée Apex Horizon Titanium',
            slug: 'montre-apex-horizon-titanium',
            description: 'Montre multisport titane',
            price: 499.00,
            stock: 12,
            category: { id: 4, name: 'Équipements & Fitness', slug: 'equipements-fitness', description: '' },
            imageUrl: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80',
            featured: true,
            rating: 4.9,
            reviewsCount: 94
          }
        }
      ]
    }
  ];

  private ordersSubject = new BehaviorSubject<Order[]>(this.loadStoredOrders());
  public orders$ = this.ordersSubject.asObservable();

  constructor(private http: HttpClient) {
    this.refreshFromApi();
  }

  private loadStoredOrders(): Order[] {
    const raw = localStorage.getItem('sv_orders');
    if (raw) {
      try { return JSON.parse(raw); } catch (e) {}
    }
    return this.defaultOrders;
  }

  private saveStoredOrders(orders: Order[]): void {
    localStorage.setItem('sv_orders', JSON.stringify(orders));
    this.ordersSubject.next(orders);
  }

  public refreshFromApi(): void {
    this.http.get<{ success: boolean; data: Order[] }>(`${this.apiUrl}/orders`)
      .pipe(
        map(res => res.data),
        catchError(() => of(this.loadStoredOrders()))
      )
      .subscribe(orders => {
        if (orders && orders.length > 0) {
          this.saveStoredOrders(orders);
        }
      });
  }

  public getOrders(): Observable<Order[]> {
    return this.orders$;
  }

  public getOrderById(id: number): Observable<Order | undefined> {
    return this.orders$.pipe(
      map(orders => orders.find(o => o.id === id))
    );
  }

  public createOrder(request: OrderCreateRequest, fullItems: { product: Product; quantity: number }[]): Observable<Order> {
    const randomSuffix = Math.random().toString(36).substring(2, 8).toUpperCase();
    const orderNum = `SV-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${randomSuffix}`;
    const total = fullItems.reduce((acc, item) => acc + (item.product.price * item.quantity), 0);

    const newOrder: Order = {
      id: Date.now(),
      orderNumber: orderNum,
      customerName: request.customerName,
      customerEmail: request.customerEmail,
      customerPhone: request.customerPhone,
      shippingAddress: request.shippingAddress,
      totalAmount: total,
      status: 'CONFIRMED',
      paymentMethod: request.paymentMethod || 'CARTE_BANCAIRE',
      createdAt: new Date().toISOString(),
      items: fullItems.map((fi, idx) => ({
        id: Date.now() + idx,
        product: fi.product,
        quantity: fi.quantity,
        unitPrice: fi.product.price
      }))
    };

    const current = [newOrder, ...this.ordersSubject.value];
    this.saveStoredOrders(current);

    this.http.post<any>(`${this.apiUrl}/orders`, request).subscribe({
      error: () => {}
    });

    return of(newOrder);
  }

  public updateStatus(id: number, status: OrderStatus): Observable<Order> {
    const current = [...this.ordersSubject.value];
    const index = current.findIndex(o => o.id === id);
    if (index > -1) {
      current[index] = { ...current[index], status };
      this.saveStoredOrders(current);

      this.http.patch<any>(`${this.apiUrl}/orders/${id}/status?status=${status}`, {}).subscribe({
        error: () => {}
      });
    }
    return of(current[index]);
  }
}
