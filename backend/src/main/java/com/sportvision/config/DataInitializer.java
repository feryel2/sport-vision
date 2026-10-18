package com.sportvision.config;

import com.sportvision.entity.*;
import com.sportvision.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final FoodRepository foodRepository;
    private final CoachRepository coachRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           BrandRepository brandRepository,
                           SupplierRepository supplierRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           ReviewRepository reviewRepository,
                           FavoriteRepository favoriteRepository,
                           FoodRepository foodRepository,
                           CoachRepository coachRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.foodRepository = foodRepository;
        this.coachRepository = coachRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            System.out.println(">>> Base de données déjà initialisée.");
            return;
        }

        System.out.println(">>> Initialisation complète Sport Vision (Gestion 3 — Boutique & Produits Sportifs)...");

        // 1. Utilisateurs démo (avec Feryel Mansouri mise à l'honneur)
        User feryel = new User(
                "feryel.mansouri@sportvision.com",
                passwordEncoder.encode("admin123"),
                "Feryel Mansouri (Responsable Gestion 3 — Boutique & Produits)",
                "+33 6 11 22 33 44",
                Role.ROLE_ADMIN
        );
        userRepository.save(feryel);

        User admin = new User(
                "admin@sportvision.com",
                passwordEncoder.encode("admin123"),
                "Alexandre Laurent (Directeur Sport Vision)",
                "+33 6 12 34 56 78",
                Role.ROLE_ADMIN
        );
        userRepository.save(admin);

        User client = new User(
                "user@sportvision.com",
                passwordEncoder.encode("user123"),
                "Karim Benzema (Athlète Client)",
                "+33 6 98 76 54 32",
                Role.ROLE_USER
        );
        userRepository.save(client);

        // 2. Marques de sport
        Brand nike = brandRepository.save(new Brand("Nike", "USA", "https://logo.clearbit.com/nike.com", "Leader mondial de l'équipement sportif haute performance."));
        Brand adidas = brandRepository.save(new Brand("Adidas", "Allemagne", "https://logo.clearbit.com/adidas.com", "Innovation et durabilité pour tous les athlètes."));
        Brand underArmour = brandRepository.save(new Brand("Under Armour", "USA", "https://logo.clearbit.com/underarmour.com", "Équipements de pointe pour la musculation et l'entraînement intensif."));
        Brand salomon = brandRepository.save(new Brand("Salomon", "France", "https://logo.clearbit.com/salomon.com", "Référence internationale en trail, running et outdoor."));
        Brand garmin = brandRepository.save(new Brand("Garmin", "Suisse", "https://logo.clearbit.com/garmin.com", "Technologies GPS, montres cardio connectées et capteurs sportifs."));
        Brand optimumNut = brandRepository.save(new Brand("Optimum Nutrition", "Irlande", "https://logo.clearbit.com/optimumnutrition.com", "Numéro 1 mondial des protéines et compléments de récupération."));

        // 3. Fournisseurs
        Supplier supSportEquip = supplierRepository.save(new Supplier("SportEquip Pro", "Lucas Martin", "contact@sportequip-pro.fr", "+33 1 45 78 90 12", "Zone Industrielle Nord, 75017 Paris", "Musculation, Haltérophilie & Fitness"));
        Supplier supEuroSport = supplierRepository.save(new Supplier("EuroSport Distribution", "Claire Dupont", "orders@eurosport-distrib.com", "+33 4 72 10 34 56", "Parc d'Activités Lyon Tech, 69007 Lyon", "Running, Chaussures & Vêtements"));
        Supplier supNutritionDirect = supplierRepository.save(new Supplier("Nutrition Direct Labs", "Jean-Marc Vasseur", "b2b@nutritiondirect.eu", "+32 2 345 67 89", "Avenue de l'Europe, 1000 Bruxelles", "Compléments alimentaires & Protéines"));
        Supplier supTechSport = supplierRepository.save(new Supplier("TechSport Logistics", "Sophie Mercier", "support@techsport-logistics.fr", "+33 4 91 22 33 44", "Terminal Fret, 13008 Marseille", "Objets connectés, Montres & Électronique"));

        // 4. Catégories du catalogue
        Category catMuscu = categoryRepository.save(new Category(
                "Musculation & Force",
                "musculation-force",
                "Haltères, bancs, bandes élastiques, ceintures lombaires et accessoires de musculation.",
                "fitness",
                "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80"
        ));

        Category catRunning = categoryRepository.save(new Category(
                "Running & Trail",
                "running-trail",
                "Chaussures avec plaque carbone, textiles respirants, sacs d'hydratation et visières.",
                "footsteps",
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80"
        ));

        Category catNutrition = categoryRepository.save(new Category(
                "Nutrition & Compléments",
                "nutrition-complements",
                "Protéines Whey Isolate, BCAA, créatines, barres énergétiques et shakers étanches.",
                "medkit",
                "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=800&q=80"
        ));

        Category catTextile = categoryRepository.save(new Category(
                "Textiles Techniques",
                "textiles-techniques",
                "Compression musculaire thermo-régulante, coupe-vents et maillots techniques.",
                "shirt",
                "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80"
        ));

        Category catAccessoires = categoryRepository.save(new Category(
                "Accessoires & Électronique",
                "accessoires-electronique",
                "Montres connectées cardio GPS, ceintures de fréquence cardiaque et brassards.",
                "watch",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80"
        ));

        // 5. Produits de la boutique (avec le cas d'usage exact : musculation -> bandes, gants, ceinture, shaker)
        Product p1 = productRepository.save(new Product(
                "Bandes élastiques de résistance (Lot de 5 niveaux)",
                "bandes-elastiques-resistance",
                "Kit complet de 5 bandes élastiques en latex 100% naturel avec résistances progressives (5kg à 30kg). Indispensable pour l'échauffement articulaire, le renforcement musculaire et le travail d'hypertrophie.",
                new BigDecimal("24.99"),
                45,
                catMuscu,
                underArmour,
                supSportEquip,
                "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?auto=format&fit=crop&w=800&q=80",
                true,
                4.9,
                38,
                "Niveaux : X-Light, Light, Medium, Heavy, X-Heavy | Matériau : Latex naturel | Sac de transport inclus",
                "Musculation"
        ));

        Product p2 = productRepository.save(new Product(
                "Gants de musculation Pro Grip en cuir renforcé",
                "gants-musculation-pro-grip",
                "Gants ergonomiques avec paume en cuir véritable renforcé et gel amortissant. Dragonne de poignet ajustable pour un maintien maximal lors des soulevés de terre et développés couchés lourds.",
                new BigDecimal("29.99"),
                32,
                catMuscu,
                nike,
                supSportEquip,
                "https://images.unsplash.com/photo-1584865288642-42078afe6942?auto=format&fit=crop&w=800&q=80",
                true,
                4.8,
                24,
                "Tailles : M, L, XL | Matériau : Cuir suédé + Mesh respirant | Fermeture velcro double tour",
                "Musculation"
        ));

        Product p3 = productRepository.save(new Product(
                "Ceinture de musculation lombaire en cuir véritable",
                "ceinture-musculation-cuir",
                "Ceinture d'haltérophilie professionnelle de 10cm de large et 10mm d'épaisseur. Offre un maintien intra-abdominal optimal pour sécuriser les vertèbres lors de squats et deadlifts lourds.",
                new BigDecimal("49.99"),
                14,
                catMuscu,
                underArmour,
                supSportEquip,
                "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80",
                true,
                4.9,
                42,
                "Épaisseur : 10mm | Boucle acier inoxydable à double ardillon | Norme IPF compétitions",
                "Musculation"
        ));

        Product p4 = productRepository.save(new Product(
                "Shaker Gold Whey Isolate 100% - Saveur Chocolat Belge (1kg)",
                "whey-isolate-gold-1kg",
                "Isolat de protéines de lactosérum natif filtré à froid. 27g de protéines ultra-pures par shaker, zéro sucre ajouté et faible en lipides pour une prise de muscle sec optimale.",
                new BigDecimal("39.99"),
                28,
                catNutrition,
                optimumNut,
                supNutritionDirect,
                "https://images.unsplash.com/photo-1593095948071-474c5cc2989d?auto=format&fit=crop&w=800&q=80",
                true,
                4.9,
                56,
                "Poids net : 1000g (33 doses) | Protéines : 90% | BCAA : 6.8g par dose | Sans gluten",
                "Musculation"
        ));

        Product p5 = productRepository.save(new Product(
                "Haltères hexagonaux en fonte caoutchoutée 20kg (Paire)",
                "halteres-hexagonaux-20kg",
                "Paire d'haltères hexagonaux anti-roulis en fonte massive recouverte d'un caoutchouc haute densité pour préserver les sols de votre espace d'entraînement.",
                new BigDecimal("119.99"),
                3, // Stock bas ! Alerte
                catMuscu,
                underArmour,
                supSportEquip,
                "https://images.unsplash.com/photo-1586401100295-7a8096fd231a?auto=format&fit=crop&w=800&q=80",
                false,
                4.7,
                15,
                "Poids : 2 x 20kg = 40kg total | Prise chromée moletée ergonomique | Forme hexagonale anti-chute",
                "Musculation"
        ));

        Product p6 = productRepository.save(new Product(
                "Chaussures Running Zoom Pegasus Carbone",
                "chaussures-running-zoom-pegasus",
                "Chaussures de running haute performance équipées d'une plaque carbone propulsive et d'une semelle en mousse réactive pour exploser vos chronos sur semi et marathon.",
                new BigDecimal("159.99"),
                22,
                catRunning,
                nike,
                supEuroSport,
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80",
                true,
                4.8,
                68,
                "Poids : 210g (pointure 42) | Drop : 8mm | Plaque carbone FlyPlate intégrale | Empeigne Flyknit",
                "Running"
        ));

        Product p7 = productRepository.save(new Product(
                "Montre GPS Forerunner Multisport & Cardio Fréquence",
                "montre-gps-forerunner-multisport",
                "Montre connectée sportive ultra-légère avec capteur optique cardiaque de 5e génération, cartographie couleur intégrée, altimètre barométrique et suivi de VMA.",
                new BigDecimal("299.99"),
                6, // Stock presque critique
                catAccessoires,
                garmin,
                supTechSport,
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80",
                true,
                4.9,
                82,
                "Autonomie : jusqu'à 14 jours (30h en mode GPS) | Étanche 5 ATM (50m) | Puce GPS multi-bandes",
                "Running"
        ));

        Product p8 = productRepository.save(new Product(
                "Veste coupe-vent imperméable Gore-Tex Trail",
                "veste-coupe-vent-trail",
                "Membrane ultra-respirante et déperlante certifiée pour les courses ultra-trail (UTMB). Coutures thermo-collées et capuche ajustée pour affronter les pires conditions.",
                new BigDecimal("89.99"),
                18,
                catTextile,
                salomon,
                supEuroSport,
                "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80",
                false,
                4.7,
                19,
                "Imperméabilité : 20 000 Schmerber | Respirabilité : 20 000 g/m²/24h | Poids plume 180g",
                "Trail"
        ));

        // 6. Aliments (Nutrition sportive)
        foodRepository.save(new Food("Blanc de poulet fermier grillé", "Protéines maigres", 165.0, 31.0, 0.0, 3.6, "100g", "Riche en acides aminés essentiels, indispensable pour la prise de masse sèche."));
        foodRepository.save(new Food("Flocons d'avoine complets bio", "Glucides complexes", 375.0, 13.5, 60.0, 7.0, "100g", "Énergie lente et diffuse, index glycémique modéré, fibres solubles prébiotiques."));
        foodRepository.save(new Food("Whey Isolate 90% Native", "Compléments", 110.0, 27.0, 0.5, 0.3, "1 dose (30g)", "Absorption rapide post-entraînement pour maximiser la synthèse protéique musculaire."));
        foodRepository.save(new Food("Banane mûre", "Fruits énergétiques", 105.0, 1.3, 27.0, 0.3, "1 pièce (120g)", "Excellente recharge glucidique avant effort, riche en potassium anti-crampes."));
        foodRepository.save(new Food("Amandes entières non grillées", "Lipides sains", 175.0, 6.0, 3.0, 15.0, "1 poignée (30g)", "Acides gras mono-insaturés, magnésium et vitamine E pour la santé cellulaire."));
        foodRepository.save(new Food("Pavé de saumon atlantique frais", "Protéines & Oméga-3", 310.0, 30.0, 0.0, 20.0, "1 portion (150g)", "Oméga-3 EPA/DHA puissamment anti-inflammatoires pour les tendons et articulations."));
        foodRepository.save(new Food("Riz basmati blanc cuit", "Glucides faciles", 195.0, 4.2, 43.0, 0.5, "1 portion (150g)", "Glucides hautement digestes idéaux pour les repas pré-entraînement."));
        foodRepository.save(new Food("Oeufs entiers plein air", "Protéines complètes", 145.0, 12.6, 1.1, 10.0, "2 gros oeufs", "Valeur biologique de référence (100) avec tous les acides aminés essentiels."));

        // 7. Coachs certifiés pour le Matching IA
        coachRepository.save(new Coach(
                "Marc Vauthier",
                "Musculation, Prise de masse & Hypertrophie",
                "Musculation",
                "Débutant",
                "Soir",
                4.95,
                45.0,
                "Coach certifié BPJEPS Haltérophilie et Musculation avec 8 ans d'expérience. Spécialiste des programmes de prise de masse propre pour débutants et intermédiaires.",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=500&q=80",
                "marc.vauthier@sportvision.com",
                "+33 6 55 44 33 22"
        ));

        coachRepository.save(new Coach(
                "Sarah Benali",
                "Running, Préparation Marathon & Cardio",
                "Running",
                "Tous niveaux",
                "Matin",
                5.0,
                50.0,
                "Ancienne coureuse d'élite de demi-fond. Accompagne les athlètes sur 10km, semi et marathon avec analyse vidéo de la foulée et gestion du rythme cardiaque.",
                "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=500&q=80",
                "sarah.benali@sportvision.com",
                "+33 6 44 33 22 11"
        ));

        coachRepository.save(new Coach(
                "Thomas Morel",
                "Force Athlétique, Powerlifting & Posture",
                "Musculation",
                "Intermédiaire",
                "Weekend",
                4.85,
                55.0,
                "Athlète compétiteur de Powerlifting. Optimisation des trois mouvements fondamentaux (Squat, Développé couché, Soulevé de terre) en toute sécurité.",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=500&q=80",
                "thomas.morel@sportvision.com",
                "+33 6 33 22 11 00"
        ));

        coachRepository.save(new Coach(
                "Julie Laurent",
                "Fitness, Remise en forme & Perte de poids",
                "Fitness",
                "Débutant",
                "Flexible",
                4.9,
                40.0,
                "Instructrice Pilates et fitness fonctionnel. Programmation douce et efficace pour perte de masse grasse, renforcement de la sangle abdominale et mobilité.",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=500&q=80",
                "julie.laurent@sportvision.com",
                "+33 6 22 11 00 99"
        ));

        coachRepository.save(new Coach(
                "Antoine Dufour",
                "Cyclisme sur route, Vélo & Endurance",
                "Cyclisme",
                "Avancé",
                "Weekend",
                4.9,
                60.0,
                "Entraîneur certifié FFC. Optimisation de la PMA, des zones de puissance et préparation des cyclosportives et étapes de montagne.",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=500&q=80",
                "antoine.dufour@sportvision.com",
                "+33 6 11 00 99 88"
        ));

        // 8. Avis clients réalistes
        reviewRepository.save(new Review(p1, "Maxime Dupont", "maxime@gmail.com", 5, "Ces bandes élastiques sont excellentes pour chauffer les épaules avant mes séances de bench ! Très résistantes."));
        reviewRepository.save(new Review(p1, "Romain Leclerc", "romain@gmail.com", 5, "Indispensable pour mes séances de musculation à la maison. Le lot de 5 permet une progression linéaire parfaite."));
        reviewRepository.save(new Review(p2, "Damien Vallet", "damien@gmail.com", 5, "Très bon maintien des poignets. Fini les ampoules lors des tractions et soulevés lourds."));
        reviewRepository.save(new Review(p3, "Alexandre V.", "alex@gmail.com", 5, "La ceinture est d'une robustesse incroyable. Elle cale parfaitement les lombaires lors des séries lourdes au squat."));
        reviewRepository.save(new Review(p4, "Karim B.", "user@sportvision.com", 5, "Très bonne miscibilité dans le shaker et goût chocolat au top, sans grumeaux."));

        // 9. Favoris initiaux
        favoriteRepository.save(new Favorite("user@sportvision.com", p1));
        favoriteRepository.save(new Favorite("user@sportvision.com", p2));
        favoriteRepository.save(new Favorite("user@sportvision.com", p3));
        favoriteRepository.save(new Favorite("feryel.mansouri@sportvision.com", p1));
        favoriteRepository.save(new Favorite("feryel.mansouri@sportvision.com", p4));

        // 10. Commandes de démonstration
        Order order1 = new Order();
        order1.setOrderNumber("ORD-2026-001");
        order1.setUser(client);
        order1.setCustomerEmail("user@sportvision.com");
        order1.setCustomerName("Karim Benzema");
        order1.setCustomerPhone("+33 6 98 76 54 32");
        order1.setShippingAddress("12 Avenue des Champs-Élysées, 75008 Paris");
        order1.setStatus(OrderStatus.DELIVERED);
        order1.setPaymentMethod("CARTE_BANCAIRE");
        order1.setTotalAmount(new BigDecimal("104.97"));
        order1.setCreatedAt(LocalDateTime.now().minusDays(5));

        List<OrderItem> items1 = new ArrayList<>();
        items1.add(new OrderItem(order1, p1, 1, p1.getPrice()));
        items1.add(new OrderItem(order1, p2, 1, p2.getPrice()));
        items1.add(new OrderItem(order1, p3, 1, p3.getPrice()));
        order1.setItems(items1);
        orderRepository.save(order1);

        System.out.println(">>> Initialisation Sport Vision terminée avec succès !");
    }
}


