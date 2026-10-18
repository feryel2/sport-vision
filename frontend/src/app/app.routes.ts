import { Routes } from '@angular/router';
import { FrontOfficeLayoutComponent } from './layouts/front-office-layout/front-office-layout.component';
import { BackOfficeLayoutComponent } from './layouts/back-office-layout/back-office-layout.component';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // FRONT-OFFICE (Boutique & Athlètes)
  {
    path: '',
    component: FrontOfficeLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./features/front-office/home/home.component').then(m => m.HomeComponent)
      },
      {
        path: 'catalogue',
        loadComponent: () => import('./features/front-office/catalog/catalog.component').then(m => m.CatalogComponent)
      },
      {
        path: 'panier',
        loadComponent: () => import('./features/front-office/cart/cart.component').then(m => m.CartComponent)
      },
      {
        path: 'favoris',
        loadComponent: () => import('./features/front-office/favorites/favorites.component').then(m => m.FavoritesComponent)
      },
      {
        path: 'coach-matching',
        loadComponent: () => import('./features/front-office/coach-matching/coach-matching.component').then(m => m.CoachMatchingComponent)
      },
      {
        path: 'aliments',
        loadComponent: () => import('./features/front-office/food-nutrition/food-nutrition.component').then(m => m.FoodNutritionComponent)
      },
      {
        path: 'contact',
        loadComponent: () => import('./features/front-office/contact/contact.component').then(m => m.ContactComponent)
      },
      {
        path: 'login',
        loadComponent: () => import('./features/front-office/login/login.component').then(m => m.LoginComponent)
      }
    ]
  },

  // BACK-OFFICE (Administration)
  {
    path: 'admin',
    component: BackOfficeLayoutComponent,
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/back-office/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'produits',
        loadComponent: () => import('./features/back-office/products-admin/products-admin.component').then(m => m.ProductsAdminComponent)
      },
      {
        path: 'categories',
        loadComponent: () => import('./features/back-office/categories-admin/categories-admin.component').then(m => m.CategoriesAdminComponent)
      },
      {
        path: 'commandes',
        loadComponent: () => import('./features/back-office/orders-admin/orders-admin.component').then(m => m.OrdersAdminComponent)
      },
      {
        path: 'aliments',
        loadComponent: () => import('./features/back-office/food-admin/food-admin.component').then(m => m.FoodAdminComponent)
      },
      {
        path: 'utilisateurs',
        loadComponent: () => import('./features/back-office/users-admin/users-admin.component').then(m => m.UsersAdminComponent)
      },
      {
        path: 'parametres',
        redirectTo: ''
      }
    ]
  },

  // Redirection globale
  {
    path: '**',
    redirectTo: ''
  }
];
