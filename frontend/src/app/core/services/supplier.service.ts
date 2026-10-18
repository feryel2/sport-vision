import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map, catchError, of } from 'rxjs';
import { Supplier } from '../models/supplier.model';

@Injectable({
  providedIn: 'root'
})
export class SupplierService {
  private apiUrl = '/api/suppliers';

  private defaultSuppliers: Supplier[] = [
    { id: 1, name: 'SportEquip Pro', contactPerson: 'Lucas Martin', email: 'contact@sportequip-pro.fr', phone: '+33 1 45 78 90 12', address: '75017 Paris', supplyCategory: 'Musculation, Haltérophilie & Fitness' },
    { id: 2, name: 'EuroSport Distribution', contactPerson: 'Claire Dupont', email: 'orders@eurosport-distrib.com', phone: '+33 4 72 10 34 56', address: '69007 Lyon', supplyCategory: 'Running, Chaussures & Vêtements' },
    { id: 3, name: 'Nutrition Direct Labs', contactPerson: 'Jean-Marc Vasseur', email: 'b2b@nutritiondirect.eu', phone: '+32 2 345 67 89', address: '1000 Bruxelles', supplyCategory: 'Compléments alimentaires & Protéines' },
    { id: 4, name: 'TechSport Logistics', contactPerson: 'Sophie Mercier', email: 'support@techsport-logistics.fr', phone: '+33 4 91 22 33 44', address: '13008 Marseille', supplyCategory: 'Objets connectés, Montres GPS & Électronique' }
  ];

  private suppliersSubject = new BehaviorSubject<Supplier[]>(this.loadSuppliers());
  public suppliers$ = this.suppliersSubject.asObservable();

  constructor(private http: HttpClient) {
    this.refresh();
  }

  private loadSuppliers(): Supplier[] {
    const saved = localStorage.getItem('sport_vision_suppliers');
    return saved ? JSON.parse(saved) : this.defaultSuppliers;
  }

  private saveSuppliers(suppliers: Supplier[]): void {
    localStorage.setItem('sport_vision_suppliers', JSON.stringify(suppliers));
    this.suppliersSubject.next(suppliers);
  }

  public refresh(): void {
    this.http.get<{ success: boolean; data: Supplier[] }>(this.apiUrl)
      .pipe(
        map(res => res.data),
        catchError(() => of(this.loadSuppliers()))
      )
      .subscribe(suppliers => {
        if (suppliers && suppliers.length > 0) {
          this.saveSuppliers(suppliers);
        }
      });
  }

  public getSuppliers(): Observable<Supplier[]> {
    return this.suppliers$;
  }

  public getSupplierById(id: number): Observable<Supplier | undefined> {
    return this.suppliers$.pipe(
      map(sups => sups.find(s => s.id === id))
    );
  }

  public createSupplier(supplier: Supplier): Observable<Supplier> {
    const newSup: Supplier = {
      ...supplier,
      id: Date.now()
    };
    this.http.post<any>(this.apiUrl, supplier).subscribe({ error: () => {} });
    const updated = [...this.suppliersSubject.value, newSup];
    this.saveSuppliers(updated);
    return of(newSup);
  }

  public updateSupplier(id: number, supplier: Supplier): Observable<Supplier> {
    const current = [...this.suppliersSubject.value];
    const index = current.findIndex(s => s.id === id);
    if (index > -1) {
      current[index] = { ...supplier, id };
      this.saveSuppliers(current);
      this.http.put<any>(`${this.apiUrl}/${id}`, supplier).subscribe({ error: () => {} });
      return of(current[index]);
    }
    return of(supplier);
  }

  public deleteSupplier(id: number): Observable<boolean> {
    const updated = this.suppliersSubject.value.filter(s => s.id !== id);
    this.saveSuppliers(updated);
    this.http.delete<any>(`${this.apiUrl}/${id}`).subscribe({ error: () => {} });
    return of(true);
  }
}
