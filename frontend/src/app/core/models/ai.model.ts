import { Product } from './product.model';
import { Coach } from './coach.model';

export interface ProductRecommendation {
  product: Product;
  matchPercentage: number;
  recommendationReason: string;
  badge: string;
}

export interface SalesForecast {
  productId: number;
  productName: string;
  categoryName: string;
  currentStock: number;
  predictedSalesNext30Days: number;
  recommendedReorderQuantity: number;
  stockHealthStatus: 'OPTIMAL' | 'RISQUE_RUPTURE' | 'CRITIQUE' | 'SURSTOCK';
  confidenceScore: number;
  trend: 'HAUSSE' | 'STABLE' | 'BAISSE';
  reason: string;
}

export interface CoachMatchingRequest {
  sport: string;
  goal: string;
  level: string;
  availability: string;
}

export interface CoachMatchingResult {
  coach: Coach;
  matchScore: number;
  compatibilityBadge: string;
  matchReasons: string[];
}
