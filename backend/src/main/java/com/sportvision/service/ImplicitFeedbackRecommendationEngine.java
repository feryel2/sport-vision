package com.sportvision.service;

import com.sportvision.dto.ProductRecommendationDto;
import com.sportvision.entity.Favorite;
import com.sportvision.entity.Order;
import com.sportvision.entity.OrderItem;
import com.sportvision.entity.OrderStatus;
import com.sportvision.entity.Product;
import com.sportvision.repository.FavoriteRepository;
import com.sportvision.repository.OrderRepository;
import com.sportvision.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A local hybrid recommender. It learns latent user/product preferences from
 * favorites and purchases with pairwise matrix factorization (BPR), and uses
 * TF-IDF content similarity to handle new users and a selected sport program.
 */
@Service
@Transactional(readOnly = true)
public class ImplicitFeedbackRecommendationEngine {
    private static final int MAX_RECOMMENDATIONS = 8;
    private static final int FACTOR_COUNT = 16;
    private static final int TRAINING_EPOCHS = 45;
    private static final double LEARNING_RATE = 0.025;
    private static final double REGULARIZATION = 0.01;

    private static final Set<String> STOP_WORDS = Set.of(
            "avec", "dans", "pour", "votre", "vos", "les", "des", "une", "sur", "par",
            "the", "and", "for", "with", "from", "this", "that", "sport", "sports"
    );

    private final ProductRepository productRepository;
    private final FavoriteRepository favoriteRepository;
    private final OrderRepository orderRepository;

    public ImplicitFeedbackRecommendationEngine(ProductRepository productRepository,
                                                FavoriteRepository favoriteRepository,
                                                OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.favoriteRepository = favoriteRepository;
        this.orderRepository = orderRepository;
    }

    public List<ProductRecommendationDto> recommend(String goal, String userEmail) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return Collections.emptyList();

        Map<Long, Product> productsById = products.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product, (first, ignored) -> first));
        Map<Long, Map<String, Integer>> productTerms = new HashMap<>();
        Map<String, Double> documentFrequency = new HashMap<>();

        for (Product product : products) {
            Map<String, Integer> terms = termCounts(productText(product));
            productTerms.put(product.getId(), terms);
            for (String term : terms.keySet()) documentFrequency.merge(term, 1.0, Double::sum);
        }

        Map<String, Double> inverseDocumentFrequency = new HashMap<>();
        for (Map.Entry<String, Double> entry : documentFrequency.entrySet()) {
            inverseDocumentFrequency.put(entry.getKey(),
                    Math.log((products.size() + 1.0) / (entry.getValue() + 1.0)) + 1.0);
        }

        Map<Long, Map<String, Double>> productVectors = new HashMap<>();
        for (Map.Entry<Long, Map<String, Integer>> entry : productTerms.entrySet()) {
            productVectors.put(entry.getKey(), tfIdf(entry.getValue(), inverseDocumentFrequency));
        }

        List<Favorite> favorites = favoriteRepository.findAll();
        List<Order> orders = orderRepository.findAll();
        Map<String, Map<Long, Double>> feedback = collectFeedback(favorites, orders, productsById);
        PairwiseFactorModel factorModel = PairwiseFactorModel.train(feedback, productsById.keySet());

        String normalizedGoal = goal == null || goal.isBlank() ? "Musculation" : goal.trim();
        Map<String, Double> userContext = new HashMap<>();
        addText(userContext, normalizedGoal, 3.0);

        if (userEmail != null && !userEmail.isBlank()) {
            Map<Long, Double> userFeedback = feedback.getOrDefault(userEmail, Collections.emptyMap());
            for (Map.Entry<Long, Double> interaction : userFeedback.entrySet()) {
                Map<String, Integer> terms = productTerms.get(interaction.getKey());
                if (terms != null) addTerms(userContext, terms, 1.0 + interaction.getValue());
            }
        }

        Map<String, Double> contextVector = tfIdf(userContext, inverseDocumentFrequency);
        boolean hasPersonalizedModel = userEmail != null && factorModel.hasLearnedUser(userEmail);
        List<ProductRecommendationDto> recommendations = new ArrayList<>();

        for (Product product : products) {
            Map<String, Double> itemVector = productVectors.get(product.getId());
            double contentSimilarity = cosineSimilarity(contextVector, itemVector);
            double score = hasPersonalizedModel
                    ? 0.65 * factorModel.predict(userEmail, product.getId()) + 0.35 * contentSimilarity
                    : contentSimilarity;

            if (score <= 0.0) continue;

            String reason = hasPersonalizedModel
                    ? "Le modèle a appris de vos favoris et achats, puis a rapproché le produit de votre programme sportif."
                    : "Le produit partage des caractéristiques avec votre programme sportif sélectionné.";
            String badge = hasPersonalizedModel ? "Profil personnalisé" : "Programme " + normalizedGoal;
            int relevance = (int) Math.round(Math.min(1.0, score) * 100.0);
            recommendations.add(new ProductRecommendationDto(product, relevance, reason, badge));
        }

        recommendations.sort(Comparator.comparingInt(ProductRecommendationDto::getMatchPercentage).reversed());
        return recommendations.stream().limit(MAX_RECOMMENDATIONS).collect(Collectors.toList());
    }

    private Map<String, Map<Long, Double>> collectFeedback(List<Favorite> favorites,
                                                            List<Order> orders,
                                                            Map<Long, Product> products) {
        Map<String, Map<Long, Double>> feedback = new HashMap<>();

        for (Favorite favorite : favorites) {
            if (favorite.getUserEmail() == null || favorite.getProduct() == null) continue;
            Long productId = favorite.getProduct().getId();
            if (products.containsKey(productId)) {
                feedback.computeIfAbsent(favorite.getUserEmail(), ignored -> new HashMap<>())
                        .merge(productId, 1.5, Double::sum);
            }
        }

        for (Order order : orders) {
            if (order.getCustomerEmail() == null || order.getStatus() == OrderStatus.CANCELLED) continue;
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null || !products.containsKey(item.getProduct().getId())) continue;
                int quantity = item.getQuantity() == null ? 1 : Math.max(1, item.getQuantity());
                double strength = 1.0 + Math.log1p(quantity);
                feedback.computeIfAbsent(order.getCustomerEmail(), ignored -> new HashMap<>())
                        .merge(item.getProduct().getId(), strength, Double::sum);
            }
        }
        return feedback;
    }

    private String productText(Product product) {
        String category = product.getCategory() == null ? "" : product.getCategory().getName();
        return String.join(" ", safe(product.getName()), safe(product.getDescription()),
                safe(product.getSportGoal()), safe(product.getSpecifications()), safe(category));
    }

    private Map<String, Integer> termCounts(String text) {
        Map<String, Integer> counts = new HashMap<>();
        String normalized = Normalizer.normalize(safe(text), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.length() < 3 || STOP_WORDS.contains(token)) continue;
            counts.merge(token, 1, Integer::sum);
        }
        return counts;
    }

    private void addText(Map<String, Double> target, String text, double weight) {
        termCounts(text).forEach((term, count) -> target.merge(term, count * weight, Double::sum));
    }

    private void addTerms(Map<String, Double> target, Map<String, Integer> terms, double weight) {
        terms.forEach((term, count) -> target.merge(term, count * weight, Double::sum));
    }

    private Map<String, Double> tfIdf(Map<String, ? extends Number> terms,
                                      Map<String, Double> inverseDocumentFrequency) {
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, ? extends Number> entry : terms.entrySet()) {
            double frequency = entry.getValue().doubleValue();
            double idf = inverseDocumentFrequency.getOrDefault(entry.getKey(), 0.0);
            if (frequency > 0.0 && idf > 0.0) {
                vector.put(entry.getKey(), (1.0 + Math.log(frequency)) * idf);
            }
        }
        return vector;
    }

    private double cosineSimilarity(Map<String, Double> first, Map<String, Double> second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) return 0.0;
        double dot = 0.0;
        double firstNorm = 0.0;
        double secondNorm = 0.0;
        for (double value : first.values()) firstNorm += value * value;
        for (double value : second.values()) secondNorm += value * value;
        for (Map.Entry<String, Double> term : first.entrySet()) {
            dot += term.getValue() * second.getOrDefault(term.getKey(), 0.0);
        }
        if (firstNorm == 0.0 || secondNorm == 0.0) return 0.0;
        return dot / (Math.sqrt(firstNorm) * Math.sqrt(secondNorm));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class PairwiseFactorModel {
        private final Map<String, Integer> users;
        private final Map<Long, Integer> items;
        private final double[][] userFactors;
        private final double[][] itemFactors;
        private final Set<String> learnedUsers;

        private PairwiseFactorModel(Map<String, Integer> users, Map<Long, Integer> items,
                                    double[][] userFactors, double[][] itemFactors,
                                    Set<String> learnedUsers) {
            this.users = users;
            this.items = items;
            this.userFactors = userFactors;
            this.itemFactors = itemFactors;
            this.learnedUsers = learnedUsers;
        }

        static PairwiseFactorModel train(Map<String, Map<Long, Double>> feedback, Set<Long> productIds) {
            Map<String, Integer> users = new LinkedHashMap<>();
            Map<Long, Integer> items = new LinkedHashMap<>();
            int itemIndex = 0;
            for (Long productId : productIds) items.put(productId, itemIndex++);
            for (String email : feedback.keySet()) users.put(email, users.size());

            double[][] userFactors = randomFactors(users.size(), FACTOR_COUNT, 17L);
            double[][] itemFactors = randomFactors(items.size(), FACTOR_COUNT, 29L);
            List<TrainingPair> positives = new ArrayList<>();
            Map<String, Set<Long>> seen = new HashMap<>();

            for (Map.Entry<String, Map<Long, Double>> user : feedback.entrySet()) {
                Set<Long> userItems = new HashSet<>(user.getValue().keySet());
                seen.put(user.getKey(), userItems);
                Integer userIndex = users.get(user.getKey());
                for (Map.Entry<Long, Double> interaction : user.getValue().entrySet()) {
                    Integer index = items.get(interaction.getKey());
                    if (index != null) positives.add(new TrainingPair(user.getKey(), userIndex, index, interaction.getValue()));
                }
            }

            Set<String> learnedUsers = new HashSet<>();
            if (items.size() > 1 && !positives.isEmpty()) {
                Random random = new Random(43L);
                for (int epoch = 0; epoch < TRAINING_EPOCHS; epoch++) {
                    Collections.shuffle(positives, random);
                    for (TrainingPair positive : positives) {
                        String email = positive.email();
                        Set<Long> userItems = seen.getOrDefault(email, Collections.emptySet());
                        int negativeIndex = sampleNegative(items, userItems, random);
                        if (negativeIndex < 0) continue;
                        update(userFactors[positive.userIndex()], itemFactors[positive.itemIndex()],
                                itemFactors[negativeIndex], positive.strength());
                        learnedUsers.add(email);
                    }
                }
            }

            return new PairwiseFactorModel(users, items, userFactors, itemFactors, learnedUsers);
        }

        boolean hasLearnedUser(String email) {
            return learnedUsers.contains(email);
        }

        double predict(String email, Long productId) {
            Integer userIndex = users.get(email);
            Integer itemIndex = items.get(productId);
            if (userIndex == null || itemIndex == null) return 0.0;
            double dot = 0.0;
            for (int factor = 0; factor < FACTOR_COUNT; factor++) {
                dot += userFactors[userIndex][factor] * itemFactors[itemIndex][factor];
            }
            return 1.0 / (1.0 + Math.exp(-dot));
        }

        private static int sampleNegative(Map<Long, Integer> items, Set<Long> seen, Random random) {
            if (seen.size() >= items.size()) return -1;
            List<Map.Entry<Long, Integer>> candidates = new ArrayList<>(items.entrySet());
            for (int attempt = 0; attempt < candidates.size() * 2; attempt++) {
                Map.Entry<Long, Integer> candidate = candidates.get(random.nextInt(candidates.size()));
                if (!seen.contains(candidate.getKey())) return candidate.getValue();
            }
            return -1;
        }

        private static void update(double[] user, double[] positive, double[] negative, double strength) {
            double difference = 0.0;
            for (int i = 0; i < FACTOR_COUNT; i++) difference += user[i] * (positive[i] - negative[i]);
            double gradient = 1.0 / (1.0 + Math.exp(difference));
            double rate = LEARNING_RATE * Math.min(3.0, Math.max(1.0, strength));

            for (int i = 0; i < FACTOR_COUNT; i++) {
                double userValue = user[i];
                double positiveValue = positive[i];
                double negativeValue = negative[i];
                user[i] += rate * (gradient * (positiveValue - negativeValue) - REGULARIZATION * userValue);
                positive[i] += rate * (gradient * userValue - REGULARIZATION * positiveValue);
                negative[i] += rate * (-gradient * userValue - REGULARIZATION * negativeValue);
            }
        }

        private static double[][] randomFactors(int rows, int columns, long seed) {
            Random random = new Random(seed);
            double[][] factors = new double[rows][columns];
            for (double[] row : factors) {
                for (int i = 0; i < row.length; i++) row[i] = (random.nextDouble() - 0.5) * 0.2;
            }
            return factors;
        }

        private record TrainingPair(String email, int userIndex, int itemIndex, double strength) {}
    }
}
