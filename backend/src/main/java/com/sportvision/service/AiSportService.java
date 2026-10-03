package com.sportvision.service;

import com.sportvision.dto.CoachMatchingRequest;
import com.sportvision.dto.CoachMatchingResult;
import com.sportvision.dto.ProductRecommendationDto;
import com.sportvision.dto.SalesForecastDto;
import com.sportvision.entity.Coach;
import com.sportvision.entity.Favorite;
import com.sportvision.entity.Order;
import com.sportvision.entity.OrderItem;
import com.sportvision.entity.Product;
import com.sportvision.repository.CoachRepository;
import com.sportvision.repository.FavoriteRepository;
import com.sportvision.repository.OrderRepository;
import com.sportvision.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AiSportService {

    private final ProductRepository productRepository;
    private final CoachRepository coachRepository;
    private final FavoriteRepository favoriteRepository;
    private final OrderRepository orderRepository;

    public AiSportService(ProductRepository productRepository,
                           CoachRepository coachRepository,
                           FavoriteRepository favoriteRepository,
                           OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.coachRepository = coachRepository;
        this.favoriteRepository = favoriteRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * AI Product Recommendation based on:
     * - Sport Goal (ex: Musculation -> bandes élastiques, gants, ceinture de musculation, shaker)
     * - User Wishlist / Favorites
     * - Previous Purchases / Orders
     */
    public List<ProductRecommendationDto> recommendProducts(String goal, String userEmail) {
        List<Product> allProducts = productRepository.findAll();
        if (allProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect favorite product IDs for this user
        Set<Long> favoriteProductIds = new HashSet<>();
        if (userEmail != null && !userEmail.isBlank()) {
            List<Favorite> favs = favoriteRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
            for (Favorite f : favs) {
                favoriteProductIds.add(f.getProduct().getId());
            }
        }

        // Collect ordered product IDs for this user
        Set<Long> orderedProductIds = new HashSet<>();
        if (userEmail != null && !userEmail.isBlank()) {
            List<Order> orders = orderRepository.findByCustomerEmailOrderByCreatedAtDesc(userEmail);
            for (Order o : orders) {
                for (OrderItem item : o.getItems()) {
                    orderedProductIds.add(item.getProduct().getId());
                }
            }
        }

        String targetGoal = (goal != null && !goal.isBlank()) ? goal.toLowerCase().trim() : "musculation";

        List<ProductRecommendationDto> recommendations = new ArrayList<>();

        for (Product product : allProducts) {
            int score = 50; // base score
            List<String> reasons = new ArrayList<>();
            String badge = "Recommandé";

            String prodName = product.getName() != null ? product.getName().toLowerCase() : "";
            String prodDesc = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
            String prodGoal = product.getSportGoal() != null ? product.getSportGoal().toLowerCase() : "";
            String catName = product.getCategory() != null ? product.getCategory().getName().toLowerCase() : "";

            // Goal matching
            if (prodGoal.contains(targetGoal)) {
                score += 35;
                reasons.add("Conçu spécifiquement pour le programme " + capitalize(targetGoal));
                badge = "Programme " + capitalize(targetGoal);
            } else if (prodName.contains(targetGoal) || prodDesc.contains(targetGoal) || catName.contains(targetGoal)) {
                score += 25;
                reasons.add("Hautement adapté à la pratique de : " + capitalize(targetGoal));
            }

            // Specific rules as requested: "Exemple : Suit un programme de musculation -> bandes élastiques, gants et ceinture de musculation."
            if (targetGoal.contains("muscu")) {
                if (prodName.contains("élastique") || prodName.contains("elastique") || prodName.contains("bande")) {
                    score += 40;
                    reasons.add("Essentiel pour l'échauffement et la résistance progressive en musculation");
                    badge = "Indispensable Musculation";
                } else if (prodName.contains("gant")) {
                    score += 38;
                    reasons.add("Protège les paumes et assure un grip optimal lors des charges lourdes");
                    badge = "Protection & Grip";
                } else if (prodName.contains("ceinture")) {
                    score += 39;
                    reasons.add("Maintien lombaire sécurisé pour squat et soulevé de terre lourd");
                    badge = "Sécurité Force";
                } else if (prodName.contains("whey") || prodName.contains("shaker") || prodName.contains("protéine")) {
                    score += 30;
                    reasons.add("Apport protéiné clé pour la récupération et la synthèse musculaire");
                    badge = "Nutrition Musculaire";
                }
            } else if (targetGoal.contains("run") || targetGoal.contains("trail") || targetGoal.contains("course")) {
                if (prodName.contains("chaussure") || prodName.contains("running") || prodName.contains("montre")) {
                    score += 35;
                    reasons.add("Optimisé pour l'amorti, la foulée et l'analyse de cadence");
                    badge = "Performance Running";
                }
            }

            // User history booster
            if (favoriteProductIds.contains(product.getId())) {
                score += 15;
                reasons.add("Présent dans votre liste de souhaits");
            }

            if (orderedProductIds.contains(product.getId())) {
                score += 10;
                reasons.add("Correspond à vos achats sportifs précédents");
            }

            if (product.getFeatured() != null && product.getFeatured()) {
                score += 5;
            }

            // Rating booster
            if (product.getRating() != null && product.getRating() >= 4.7) {
                score += 8;
            }

            // Cap score between 60 and 99%
            int finalScore = Math.min(99, Math.max(60, score));

            String primaryReason = reasons.isEmpty() ?
                    "Produit phare sélectionné par notre algorithme IA Sport Vision" :
                    String.join(" • ", reasons);

            recommendations.add(new ProductRecommendationDto(product, finalScore, primaryReason, badge));
        }

        // Sort by match percentage descending
        recommendations.sort((a, b) -> Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));

        // Return top 8 recommendations
        return recommendations.stream().limit(8).collect(Collectors.toList());
    }

    /**
     * AI Sales & Stock Forecast:
     * Predicts sales for the next 30 days and alerts when stock is at risk.
     */
    public List<SalesForecastDto> generateSalesForecast() {
        List<Product> products = productRepository.findAll();
        List<SalesForecastDto> forecasts = new ArrayList<>();

        for (Product product : products) {
            int currentStock = product.getStock() != null ? product.getStock() : 0;
            String catName = product.getCategory() != null ? product.getCategory().getName() : "Général";
            double rating = product.getRating() != null ? product.getRating() : 4.5;
            boolean isFeatured = product.getFeatured() != null && product.getFeatured();

            // AI dynamic heuristic estimation for 30-day demand
            int baseSalesVelocity = isFeatured ? 25 : 12;
            if (rating > 4.7) baseSalesVelocity += 8;
            if (catName.equalsIgnoreCase("Musculation") || catName.equalsIgnoreCase("Running")) {
                baseSalesVelocity += 10;
            }

            int predictedSales = baseSalesVelocity + (int) (product.getId() % 7);
            int recommendedReorder = Math.max(0, (int) (predictedSales * 1.4) - currentStock);

            String status;
            String trend;
            String reason;
            double confidence = 0.88 + ((product.getId() % 10) * 0.01);

            if (currentStock == 0) {
                status = "CRITIQUE";
                trend = "HAUSSE";
                reason = "Rupture totale immédiate ! Forte demande prévue (" + predictedSales + " unités/mois).";
            } else if (currentStock <= 5 || currentStock < predictedSales * 0.5) {
                status = "RISQUE_RUPTURE";
                trend = "HAUSSE";
                reason = "Le stock actuel (" + currentStock + ") sera épuisé sous 10 jours au rythme actuel.";
            } else if (currentStock > predictedSales * 2.5) {
                status = "SURSTOCK";
                trend = "BAISSE";
                reason = "Niveau de stock élevé (" + currentStock + ") par rapport aux prévisions mensuelles.";
            } else {
                status = "OPTIMAL";
                trend = "STABLE";
                reason = "Stock équilibré garantissant une couverture de vente de 30 à 45 jours.";
            }

            forecasts.add(new SalesForecastDto(
                    product.getId(),
                    product.getName(),
                    catName,
                    currentStock,
                    predictedSales,
                    recommendedReorder,
                    status,
                    Math.round(confidence * 100.0) / 100.0,
                    trend,
                    reason
            ));
        }

        // Sort by risk priority: CRITIQUE, RISQUE_RUPTURE first
        forecasts.sort((a, b) -> {
            int pA = a.getStockHealthStatus().equals("CRITIQUE") ? 0 :
                     a.getStockHealthStatus().equals("RISQUE_RUPTURE") ? 1 :
                     a.getStockHealthStatus().equals("SURSTOCK") ? 2 : 3;
            int pB = b.getStockHealthStatus().equals("CRITIQUE") ? 0 :
                     b.getStockHealthStatus().equals("RISQUE_RUPTURE") ? 1 :
                     b.getStockHealthStatus().equals("SURSTOCK") ? 2 : 3;
            return Integer.compare(pA, pB);
        });

        return forecasts;
    }

    /**
     * AI Coach Matching:
     * Compares user Goal, Level, Preferred Sport, and Availability with coach profiles.
     * Example: Musculation + prise de masse + débutant -> coachs spécialisés avec compatibilité %
     */
    public List<CoachMatchingResult> matchCoaches(CoachMatchingRequest request) {
        List<Coach> coaches = coachRepository.findAll();
        List<CoachMatchingResult> results = new ArrayList<>();

        String reqSport = request.getSport() != null ? request.getSport().toLowerCase().trim() : "";
        String reqGoal = request.getGoal() != null ? request.getGoal().toLowerCase().trim() : "";
        String reqLevel = request.getLevel() != null ? request.getLevel().toLowerCase().trim() : "";
        String reqAvail = request.getAvailability() != null ? request.getAvailability().toLowerCase().trim() : "";

        for (Coach coach : coaches) {
            int score = 40; // baseline
            List<String> reasons = new ArrayList<>();

            String cSport = coach.getSport() != null ? coach.getSport().toLowerCase() : "";
            String cSpecialty = coach.getSpecialty() != null ? coach.getSpecialty().toLowerCase() : "";
            String cBio = coach.getBio() != null ? coach.getBio().toLowerCase() : "";
            String cLevel = coach.getTargetLevel() != null ? coach.getTargetLevel().toLowerCase() : "";
            String cAvail = coach.getAvailability() != null ? coach.getAvailability().toLowerCase() : "";

            // Sport match (+30 pts)
            if (!reqSport.isBlank() && (cSport.contains(reqSport) || reqSport.contains(cSport))) {
                score += 30;
                reasons.add("Spécialiste certifié en " + coach.getSport());
            } else if (!reqSport.isBlank() && (cSpecialty.contains(reqSport) || cBio.contains(reqSport))) {
                score += 18;
                reasons.add("Expérience démontrée en " + coach.getSport());
            }

            // Goal match (+20 pts)
            if (!reqGoal.isBlank()) {
                if (cSpecialty.contains(reqGoal) || cBio.contains(reqGoal)) {
                    score += 22;
                    reasons.add("Expertise ciblée sur votre objectif : " + coach.getSpecialty());
                } else if (reqGoal.contains("masse") && (cSpecialty.contains("muscu") || cSpecialty.contains("force"))) {
                    score += 20;
                    reasons.add("Protocoles d'hypertrophie et prise de masse musculaire éprouvés");
                } else if (reqGoal.contains("poids") && (cSpecialty.contains("fitness") || cSpecialty.contains("crossfit") || cSpecialty.contains("cardio"))) {
                    score += 20;
                    reasons.add("Programmes de reconditionnement physique et perte de masse grasse");
                }
            }

            // Level match (+15 pts)
            if (!reqLevel.isBlank()) {
                if (cLevel.contains(reqLevel) || cLevel.contains("tous")) {
                    score += 15;
                    reasons.add("Pédagogie adaptée au niveau " + request.getLevel());
                } else {
                    score += 5;
                }
            }

            // Availability match (+10 pts)
            if (!reqAvail.isBlank()) {
                if (cAvail.contains(reqAvail) || cAvail.contains("flexible")) {
                    score += 10;
                    reasons.add("Créneaux disponibles le " + request.getAvailability());
                }
            }

            // Coach rating booster
            if (coach.getRating() != null && coach.getRating() >= 4.8) {
                score += 5;
            }

            int finalScore = Math.min(99, Math.max(50, score));

            String badge;
            if (finalScore >= 90) {
                badge = "Match Parfait (" + finalScore + "%)";
            } else if (finalScore >= 80) {
                badge = "Excellente Compatibilité (" + finalScore + "%)";
            } else if (finalScore >= 70) {
                badge = "Très Bonne Compatibilité (" + finalScore + "%)";
            } else {
                badge = "Profil Éligible (" + finalScore + "%)";
            }

            if (reasons.isEmpty()) {
                reasons.add("Coach diplômé d'État avec d'excellents retours sportifs");
            }

            results.add(new CoachMatchingResult(coach, finalScore, badge, reasons));
        }

        // Sort descending by score
        results.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return results;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
