package com.sportvision.training.controller;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.TrainingAiDto.*;
import com.sportvision.training.service.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/training/ai")
public class TrainingAiController {
    private final TrainingAiService service;
    private final TrainingAlternativeService alternatives;
    public TrainingAiController(TrainingAiService service,TrainingAlternativeService alternatives){this.service=service;this.alternatives=alternatives;}
    @PostMapping("/programs/{id}/alternative") public ApiResponse<TrainingAlternativeDto.Preview> alternative(@PathVariable long id,@Valid @RequestBody TrainingAlternativeDto.Request input){
        return ApiResponse.ok("Alternative validée. Programme inchangé.",alternatives.propose(id,input));
    }
    @PostMapping("/programs/{id}/replacement") public ApiResponse<ProgramOutput> replace(@PathVariable long id,@Valid @RequestBody TrainingAlternativeDto.Confirmation input){
        return ApiResponse.ok("Remplacement enregistré dans une nouvelle révision.",alternatives.confirm(id,input));
    }
    @GetMapping("/options") public ApiResponse<Options> options(){return ApiResponse.ok("Préférences IA chargées.",service.options());}
    @PostMapping("/generate") public ApiResponse<Preview> generate(@Valid @RequestBody Preferences preferences){
        return ApiResponse.ok("Proposition validée. Aucun programme encore enregistré.",service.generate(preferences));
    }
    @PostMapping("/programs") public ResponseEntity<ApiResponse<ProgramOutput>> save(@Valid @RequestBody Save input){
        return ResponseEntity.status(201).body(ApiResponse.ok("Programme enregistré.",service.save(input)));
    }
    @ExceptionHandler(TrainingAiException.class)
    public ResponseEntity<?> failure(TrainingAiException ex){
        return ResponseEntity.status(ex.getStatus()).body(new ApiResponse<>(false,ex.getMessage(),Map.of("code",ex.getCode(),"errors",Map.of())));
    }
}
