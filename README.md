# Sport Vision &bull; Plateforme Full-Stack Spring Boot & Angular

> **Projet :** Sport Vision - Performance, Optique Haute Précision & Équipements Sportifs  
> **Technologies :** Spring Boot 3.2 (Java 17) &bull; Spring Data JPA &bull; Spring Security &bull; Base H2 / MySQL XAMPP &bull; Angular 17 &bull; TypeScript &bull; Standalone Components &bull; Charte Graphique Bleu & Blanc

---

## 1. Présentation du Projet

**Sport Vision** est une application web full-stack d'excellence dédiée à la distribution d'équipements sportifs haut de gamme et d'optique sportive de précision (lunettes photochromiques, verres polarisés haute définition, textiles thermo-régulants, chaussures de compétition à plaque carbone et accessoires connectés).

L'application a été conçue avec une stricte séparation des responsabilités et intègre :
- Un **Backend Spring Boot** fournissant une API REST complète, documentée et sécurisée.
- Un **Frontend Angular** moderne avec deux templates intégrés :
  - **Front-Office :** Vitrine athlétique immersive, catalogue filtrable en temps réel, panier interactif avec jauge de livraison offerte, présentation du laboratoire d'innovation et espace de connexion démo.
  - **Back-Office :** Console d'administration complète avec KPI en temps réel (chiffre d'affaires, commandes, alertes stocks critiques), graphiques CSS de vente, gestion CRUD des équipements, gestion des commandes clients et des catégories.
- Une **Charte Graphique exclusive Bleu et Blanc** combinant le Bleu Royal Sportif (`#1D4ED8`), le Bleu Azur High-Tech (`#0284C7`), le Bleu Nuit Profond (`#0F172A`) et un Blanc Pur contrasté (`#FFFFFF` / `#F8FAFC`).

---

## 2. Identifiants de Démonstration (Connexion en 1 Clic)

Pour tester immédiatement l'ensemble des fonctionnalités sans saisie manuelle, des boutons de remplissage rapide sont intégrés sur la page [`/login`](http://localhost:4200/login) :

| Profil | Email | Mot de passe | Rôle & Périmètre |
|---|---|---|---|
| **Administrateur / Direction** | `admin@sportvision.com` | `admin123` | Accès Back-Office complet, CRUD équipements, modification des statuts de commande, indicateurs KPI |
| **Athlète / Client Démo** | `client@sportvision.com` | `client123` | Accès Front-Office, navigation catalogue, ajout au panier, simulation de commande avec livraison offerte |

---

## 3. Guide de Démarrage Rapide

### Prérequis
- **Java 17+** (Vérifiez avec `java -version`)
- **Node.js 18+** (Vérifiez avec `node -v`)

---

### Démarrage du Backend (Spring Boot)

Le backend est préconfiguré avec une base de données **H2 en mémoire** avec console web activée. Il démarre instantanément sans aucun logiciel tiers requis !

1. Ouvrez un terminal dans le dossier `backend` :
   ```bash
   cd sport-vision/backend
   ```
2. Lancez l'application avec le wrapper Maven ou votre Maven local :
   - Sous Windows :
     ```cmd
     mvnw.cmd spring-boot:run
     ```
     *(ou `mvn spring-boot:run` si Maven est installé dans votre PATH)*
3. L'API est accessible sur : **`http://localhost:8080/api`**
4. La console H2 est accessible sur : **`http://localhost:8080/h2-console`**
   - **JDBC URL :** `jdbc:h2:mem:sportvisiondb`
   - **User :** `sa`
   - **Password :** *(laisser vide)*

> 💡 **Option MySQL / XAMPP :** Si vous souhaitez persister les données dans MySQL via XAMPP, il vous suffit de décommenter la section MySQL dans [`src/main/resources/application.properties`](file:///c:/xampp/htdocs/fer/sport-vision/backend/src/main/resources/application.properties).

---

### Démarrage du Frontend (Angular)

1. Ouvrez un second terminal dans le dossier `frontend` :
   ```bash
   cd sport-vision/frontend
   ```
2. Installez les dépendances :
   ```cmd
   npm install
   ```
3. Lancez le serveur de développement Angular :
   ```cmd
   npm start
   ```
   *(ou `npx ng serve`)*
4. Ouvrez votre navigateur sur : **`http://localhost:4200`**

---

## 4. Architecture Détaillée

```
sport-vision/
├── backend/
│   ├── src/main/java/com/sportvision/
│   │   ├── config/
│   │   │   ├── WebConfig.java          # Configuration CORS (http://localhost:4200)
│   │   │   ├── SecurityConfig.java     # Sécurisation Spring Security (BCrypt, CSRF REST)
│   │   │   └── DataInitializer.java    # Injection démo (Admin, Client, 15 équipements, 3 commandes)
│   │   ├── controller/
│   │   │   ├── AuthController.java     # Inscription, connexion, utilisateurs
│   │   │   ├── ProductController.java  # CRUD produits, recherche, filtre catégorie, stock
│   │   │   ├── CategoryController.java # CRUD catégories de sport
│   │   │   ├── OrderController.java    # Commandes, création panier, statuts
│   │   │   └── StatsController.java    # Données analytiques et KPI du dashboard
│   │   ├── dto/                        # Objets d'échange de données
│   │   ├── entity/                     # Entités JPA (User, Product, Category, Order, OrderItem)
│   │   ├── repository/                 # Repositories Spring Data JPA
│   │   ├── service/                    # Logique métier & transactions
│   │   └── SportVisionApplication.java # Classe principale
│   ├── src/main/resources/
│   │   └── application.properties      # Configuration H2 / MySQL
│   ├── pom.xml
│   └── mvnw.cmd
│
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/
│   │   │   │   ├── models/             # Modèles TypeScript (Product, Category, Order, User, Stats)
│   │   │   │   └── services/           # Services (ProductService, CartService, AuthService, OrderService, StatsService)
│   │   │   ├── layouts/
│   │   │   │   ├── front-office-layout/ # Navbar publique, panier, switch démo, footer complet
│   │   │   │   └── back-office-layout/  # Sidebar rétractable Bleu Nuit, topbar admin, profil
│   │   │   ├── features/
│   │   │   │   ├── front-office/
│   │   │   │   │   ├── home/           # Hero haute énergie, rayons, best-sellers, Vision Lab
│   │   │   │   │   ├── catalog/        # Filtres dynamiques sport/prix, recherche instantanée
│   │   │   │   │   ├── cart/           # Panier, jauge livraison offerte, formulaire de commande
│   │   │   │   │   ├── contact/        # Formulaire d'assistance, FAQ, coordonnées
│   │   │   │   │   └── login/          # Connexion / Inscription avec boutons démo 1-clic
│   │   │   │   └── back-office/
│   │   │   │       ├── dashboard/      # 4 KPI cards, graphiques de vente, commandes récentes, alertes stock
│   │   │   │       ├── products-admin/ # CRUD complet avec table, modales d'ajout/édition/suppression
│   │   │   │       ├── orders-admin/   # Suivi des commandes et mise à jour des statuts
│   │   │   │       ├── categories-admin/# Gestion des disciplines sportives
│   │   │   │       └── users-admin/    # Gestion des comptes et des rôles
│   │   │   ├── app.routes.ts           # Routage complet Front/Back
│   │   │   ├── app.config.ts
│   │   │   └── app.component.ts
│   │   ├── assets/logo.svg             # Logo vectoriel officiel Sport Vision
│   │   ├── styles.css                  # Charte graphique globale Bleu & Blanc
│   │   └── index.html
│   ├── package.json
│   ├── angular.json
│   └── tsconfig.json
└── README.md
```

---

## 5. Endpoints Principaux de l'API REST

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authentification utilisateur & token |
| `POST` | `/api/auth/register` | Inscription d'un nouvel athlète |
| `GET` | `/api/products` | Récupérer la liste des produits (avec `?query=` et `?categoryId=`) |
| `GET` | `/api/products/featured` | Récupérer les produits vedettes (best-sellers) |
| `GET` | `/api/products/low-stock` | Récupérer les produits en stock critique |
| `POST` | `/api/products` | Ajouter un nouvel équipement (Back-Office) |
| `PUT` | `/api/products/{id}` | Modifier un équipement existant |
| `DELETE` | `/api/products/{id}` | Supprimer un équipement |
| `GET` | `/api/categories` | Liste des disciplines sportives |
| `POST` | `/api/categories` | Créer une nouvelle catégorie |
| `GET` | `/api/orders` | Liste des commandes clients |
| `POST` | `/api/orders` | Créer une commande à partir du panier |
| `PATCH` | `/api/orders/{id}/status` | Mettre à jour le statut d'une commande |
| `GET` | `/api/stats/dashboard` | KPI et statistiques pour le Back-Office |
