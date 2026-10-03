import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { Product, ProductFormData } from '../models/product.model';
import { Category } from '../models/category.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = 'http://localhost:8080/api';

  // Catégories par défaut Sport Vision
  private defaultCategories: Category[] = [
    {
      id: 1,
      name: 'Musculation & Force',
      slug: 'musculation-force',
      description: 'Haltères, bancs, bandes élastiques, ceintures lombaires et accessoires de musculation.',
      icon: 'fitness',
      imageUrl: 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: 2,
      name: 'Running & Trail',
      slug: 'running-trail',
      description: 'Chaussures plaque carbone, textiles respirants, sacs d\'hydratation et visières.',
      icon: 'footsteps',
      imageUrl: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: 3,
      name: 'Nutrition & Compléments',
      slug: 'nutrition-complements',
      description: 'Protéines Whey Isolate, BCAA, créatines, barres énergétiques et shakers étanches.',
      icon: 'medkit',
      imageUrl: 'https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: 4,
      name: 'Textiles Techniques',
      slug: 'textiles-techniques',
      description: 'Compression musculaire thermo-régulante, coupe-vents déperlants et cuissards.',
      icon: 'shirt',
      imageUrl: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: 5,
      name: 'Accessoires & Électronique',
      slug: 'accessoires-electronique',
      description: 'Montres GPS connectées, cardiofréquencemètres et ceintures de course.',
      icon: 'watch',
      imageUrl: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80'
    }
  ];

  // Catalogue complet Sport Vision
  private defaultProducts: Product[] = [
    {
      id: 1,
      name: 'Bandes élastiques de résistance (Lot de 5 niveaux)',
      slug: 'bandes-elastiques-resistance',
      description: 'Kit de 5 bandes élastiques en latex 100% naturel de 5kg à 30kg. Idéal pour échauffement articulaire et hypertrophie musculaire.',
      price: 24.99,
      stock: 45,
      category: this.defaultCategories[0],
      imageUrl: 'https://images.unsplash.com/photo-1598971639058-fab3c3109a00?auto=format&fit=crop&w=800&q=80',
      featured: true,
      rating: 4.9,
      reviewsCount: 38,
      sportGoal: 'Musculation',
      specifications: 'Latex naturel indéchirable • 5 résistances • Sacoche incluse'
    },
    {
      id: 2,
      name: 'Gants de musculation Pro Grip en cuir renforcé',
      slug: 'gants-musculation-pro-grip',
      description: 'Gants ergonomiques avec paume en cuir véritable renforcé et gel anti-vibration. Dragonne ajustable pour charges lourdes.',
      price: 29.99,
      stock: 32,
      category: this.defaultCategories[0],
      imageUrl: 'https://images.unsplash.com/photo-1584865288642-42078afe6942?auto=format&fit=crop&w=800&q=80',
      featured: true,
      rating: 4.8,
      reviewsCount: 24,
      sportGoal: 'Musculation',
      specifications: 'Cuir suédé • Double tour de poignet • Mesh aéré'
    },
    {
      id: 3,
      name: 'Ceinture de musculation lombaire en cuir véritable',
      slug: 'ceinture-musculation-cuir',
      description: 'Ceinture d\'haltérophilie professionnelle de 10cm de large et 10mm d\'épaisseur pour squats et soulevés de terre lourds.',
      price: 49.99,
      stock: 14,
      category: this.defaultCategories[0],
      imageUrl: 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80',
      featured: true,
      rating: 4.9,
      reviewsCount: 42,
      sportGoal: 'Musculation',
      specifications: 'Épaisseur 10mm • Boucle double ardillon inox • Normes IPF'
    },
    {
      id: 4,
      name: 'Shaker Gold Whey Isolate 100% - Chocolat (1kg)',
      slug: 'whey-isolate-gold-1kg',
      description: 'Isolat de lactosérum natif à 90% de protéines pures. Zéro sucre ajouté, dissolution instantanée sans grumeaux.',
      price: 39.99,
      stock: 28,
      category: this.defaultCategories[2],
      imageUrl: 'https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=800&q=80',
      featured: true,
      rating: 4.9,
      reviewsCount: 56,
      sportGoal: 'Musculation',
      specifications: '1000g (33 doses) • 27g protéines/dose • 6.8g BCAA'
    },
    {
      id: 5,
      name: 'Haltères hexagonaux 20kg (Paire)',
      slug: 'halteres-hexagonaux-20kg',
      description: 'Paire d\'haltères en fonte pleine revêtus de caoutchouc haute densité absorbant les chocs et anti-roulis.',
      price: 119.99,
      stock: 3, // Stock bas
      category: this.defaultCategories[0],
      imageUrl: 'https://images.unsplash.com/photo-1586401100295-7a8096fd231a?auto=format&fit=crop&w=800&q=80',
      featured: false,
      rating: 4.7,
      reviewsCount: 15,
      sportGoal: 'Musculation',
      specifications: 'Poids 2x20kg • Poignées chromées ergonomiques'
    },
    {
      id: 6,
      name: 'Chaussures Running Zoom Pegasus Carbone',
      slug: 'chaussures-running-zoom-pegasus',
      description: 'Chaussures de running avec plaque carbone propulsive et amorti dynamique pour marathons et sorties longues.',
      price: 159.99,
      stock: 22,
      category: this.defaultCategories[1],
      imageUrl: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80',
      featured: true,
      rating: 4.8,
      reviewsCount: 68,
      sportGoal: 'Running',
      specifications: 'Plaque carbone intégrale • Poids 210g • Drop 8mm'
    },
    {
      id: 7,
      name: 'Montre GPS Forerunner Cardiofréquencemètre',
      slug: 'montre-gps-forerunner-multisport',
      description: 'Montre sportive connectée ultralégère avec capteur cardio optique, suivi de cadence, GPS multi-bandes et alertes de récupération.',
      price: 299.99,
      stock: 6,
      category: this.defaultCategories[4],
      imageUrl: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80',
      featured: true,
      rating: 4.9,
      reviewsCount: 82,
      sportGoal: 'Running',
      specifications: 'Autonomie 14j • Étanche 5 ATM • GPS multi-système'
    },
    {
      id: 8,
      name: 'Veste coupe-vent imperméable Gore-Tex Trail',
      slug: 'veste-coupe-vent-trail',
      description: 'Veste ultra-respirante et imperméable homologuée courses de montagne (UTMB). Coutures étanches thermo-soudées.',
      price: 89.99,
      stock: 18,
      category: this.defaultCategories[3],
      imageUrl: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80',
      featured: false,
      rating: 4.7,
      reviewsCount: 19,
      sportGoal: 'Trail',
      specifications: '20 000 Schmerber • Poids 180g • Capuche ajustable'
    }
  ];

  private productsSubject = new BehaviorSubject<Product[]>(this.loadStoredProducts());
  public products$ = this.productsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.refreshFromApi();
  }

  private loadStoredProducts(): Product[] {
    const saved = localStorage.getItem('sport_vision_products');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        return this.defaultProducts;
      }
    }
    return this.defaultProducts;
  }

  private saveStoredProducts(products: Product[]): void {
    localStorage.setItem('sport_vision_products', JSON.stringify(products));
    this.productsSubject.next(products);
  }

  public refreshFromApi(): void {
    this.http.get<{ success: boolean; data: Product[] }>(`${this.apiUrl}/products`)
      .pipe(
        map(res => res.data),
        catchError(() => of(this.loadStoredProducts()))
      )
      .subscribe(products => {
        if (products && products.length > 0) {
          this.saveStoredProducts(products);
        }
      });
  }

  public getCategories(): Observable<Category[]> {
    return this.http.get<{ success: boolean; data: Category[] }>(`${this.apiUrl}/categories`)
      .pipe(
        map(res => res.data),
        catchError(() => of(this.defaultCategories))
      );
  }

  public getProducts(categoryId?: number, query?: string, goal?: string): Observable<Product[]> {
    return this.products$.pipe(
      map(products => {
        return products.filter(p => {
          const matchCat = categoryId ? p.category && p.category.id === categoryId : true;
          const matchQuery = query ?
            p.name.toLowerCase().includes(query.toLowerCase()) ||
            p.description.toLowerCase().includes(query.toLowerCase()) : true;
          const matchGoal = goal ?
            (p.sportGoal && p.sportGoal.toLowerCase().includes(goal.toLowerCase())) : true;
          return matchCat && matchQuery && matchGoal;
        });
      })
    );
  }

  public getFeaturedProducts(): Observable<Product[]> {
    return this.products$.pipe(
      map(products => products.filter(p => p.featured))
    );
  }

  public getLowStock(threshold: number = 5): Observable<Product[]> {
    return this.products$.pipe(
      map(products => products.filter(p => p.stock <= threshold))
    );
  }

  public getProductById(id: number): Observable<Product | undefined> {
    return this.products$.pipe(
      map(products => products.find(p => p.id === id))
    );
  }

  public createProduct(data: ProductFormData): Observable<Product> {
    const category = this.defaultCategories.find(c => c.id === Number(data.categoryId)) || this.defaultCategories[0];
    const newProduct: Product = {
      id: Date.now(),
      name: data.name,
      slug: data.slug || data.name.toLowerCase().replace(/\s+/g, '-'),
      description: data.description,
      price: Number(data.price),
      stock: Number(data.stock),
      category: category,
      sportGoal: data.sportGoal || 'Musculation',
      imageUrl: data.imageUrl || 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80',
      featured: data.featured,
      rating: 4.8,
      reviewsCount: 1,
      specifications: data.specifications || 'Conception Sport Vision Haute Précision'
    };

    this.http.post<any>(`${this.apiUrl}/products`, data).subscribe({ error: () => {} });

    const updated = [newProduct, ...this.productsSubject.value];
    this.saveStoredProducts(updated);
    return of(newProduct);
  }

  public updateProduct(id: number, data: ProductFormData): Observable<Product> {
    const current = [...this.productsSubject.value];
    const index = current.findIndex(p => p.id === id);
    if (index === -1) throw new Error('Produit introuvable');

    const category = this.defaultCategories.find(c => c.id === Number(data.categoryId)) || current[index].category;

    const updatedProduct: Product = {
      ...current[index],
      name: data.name,
      description: data.description,
      price: Number(data.price),
      stock: Number(data.stock),
      category: category,
      sportGoal: data.sportGoal || current[index].sportGoal,
      imageUrl: data.imageUrl,
      featured: data.featured,
      specifications: data.specifications
    };

    current[index] = updatedProduct;
    this.saveStoredProducts(current);

    this.http.put<any>(`${this.apiUrl}/products/${id}`, data).subscribe({ error: () => {} });

    return of(updatedProduct);
  }

  public updatePrice(id: number, price: number): Observable<Product> {
    const current = [...this.productsSubject.value];
    const index = current.findIndex(p => p.id === id);
    if (index > -1) {
      current[index] = { ...current[index], price: Number(price) };
      this.saveStoredProducts(current);
      this.http.patch<any>(`${this.apiUrl}/products/${id}/price?price=${price}`, {}).subscribe({ error: () => {} });
      return of(current[index]);
    }
    throw new Error('Produit introuvable');
  }

  public updateStock(id: number, stock: number): Observable<Product> {
    const current = [...this.productsSubject.value];
    const index = current.findIndex(p => p.id === id);
    if (index > -1) {
      current[index] = { ...current[index], stock: Math.max(0, stock) };
      this.saveStoredProducts(current);
      this.http.patch<any>(`${this.apiUrl}/products/${id}/stock?stock=${stock}`, {}).subscribe({ error: () => {} });
      return of(current[index]);
    }
    throw new Error('Produit introuvable');
  }

  public adjustStock(id: number, delta: number): Observable<Product> {
    const current = [...this.productsSubject.value];
    const index = current.findIndex(p => p.id === id);
    if (index > -1) {
      const newStock = Math.max(0, current[index].stock + delta);
      current[index] = { ...current[index], stock: newStock };
      this.saveStoredProducts(current);
      this.http.post<any>(`${this.apiUrl}/products/${id}/stock/adjust?delta=${delta}`, {}).subscribe({ error: () => {} });
      return of(current[index]);
    }
    throw new Error('Produit introuvable');
  }

  public deleteProduct(id: number): Observable<boolean> {
    const current = this.productsSubject.value.filter(p => p.id !== id);
    this.saveStoredProducts(current);
    this.http.delete<any>(`${this.apiUrl}/products/${id}`).subscribe({ error: () => {} });
    return of(true);
  }
}
