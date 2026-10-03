package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.dto.CoachMatchingRequest;
import com.sportvision.dto.CoachMatchingResult;
import com.sportvision.dto.ProductRecommendationDto;
import com.sportvision.dto.SalesForecastDto;
import com.sportvision.entity.Coach;
import com.sportvision.repository.CoachRepository;
import com.sportvision.service.AiSportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiSportController {

    private final AiSportService aiSportService;
    private final CoachRepository coachRepository;

    public AiSportController(AiSportService aiSportService, CoachRepository coachRepository) {
        this.aiSportService = aiSportService;
        this.coachRepository = coachRepository;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<ProductRecommendationDto>>> getProductRecommendations(
            @RequestParam(defaultValue = "Musculation") String goal,
            @RequestParam(required = false) String userEmail) {
        List<ProductRecommendationDto> recs = aiSportService.recommendProducts(goal, userEmail);
        return ResponseEntity.ok(ApiResponse.ok(recs));
    }

    @GetMapping("/sales-forecast")
    public ResponseEntity<ApiResponse<List<SalesForecastDto>>> getSalesForecast() {
        List<SalesForecastDto> forecast = aiSportService.generateSalesForecast();
        return ResponseEntity.ok(ApiResponse.ok(forecast));
    }

    @PostMapping("/coach-matching")
    public ResponseEntity<ApiResponse<List<CoachMatchingResult>>> matchCoaches(
            @RequestBody CoachMatchingRequest request) {
        List<CoachMatchingResult> results = aiSportService.matchCoaches(request);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/coaches")
    public ResponseEntity<ApiResponse<List<Coach>>> getAllCoaches() {
        return ResponseEntity.ok(ApiResponse.ok(coachRepository.findAll()));
    }
}
