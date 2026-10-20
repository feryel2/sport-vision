package com.sportvision.training.controller;
import com.fasterxml.jackson.databind.JsonNode;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.TrainingAdaptationDto.*;
import com.sportvision.training.service.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/training/ai/programs/{id}/adaptation")
public class TrainingAdaptationController {
    private final TrainingAdaptationService service;
    public TrainingAdaptationController(TrainingAdaptationService service){this.service=service;}
    @GetMapping public ApiResponse<Report> report(@PathVariable long id,@RequestParam int expectedRevision){return ApiResponse.ok("Bilan calculé.",service.report(id,expectedRevision));}
    @PostMapping public ApiResponse<Preview> propose(@PathVariable long id,@RequestBody JsonNode input){
        check(input,Set.of("expectedRevision"));return ApiResponse.ok("Proposition validée. Programme inchangé.",service.propose(id,revision(input)));
    }
    @PostMapping("/confirm") public ApiResponse<ProgramOutput> confirm(@PathVariable long id,@RequestBody JsonNode input){
        check(input,Set.of("expectedRevision","proposalId"));UUID token;
        try{if(!input.path("proposalId").isTextual())throw new IllegalArgumentException();token=UUID.fromString(input.path("proposalId").asText());}
        catch(IllegalArgumentException ex){throw new ProgramException(400,"Identifiant de proposition invalide.");}
        return ApiResponse.ok("Adaptation enregistrée dans une nouvelle révision.",service.confirm(id,revision(input),token));
    }
    private int revision(JsonNode input){var r=input.path("expectedRevision");if(!r.isIntegralNumber()||!r.canConvertToInt()||r.intValue()<1)throw new ProgramException(400,"Révision invalide.");return r.intValue();}
    private void check(JsonNode input,Set<String> allowed){
        if(input==null||!input.isObject()||input.size()!=allowed.size())throw new ProgramException(400,"Requête d’adaptation invalide.");
        input.fieldNames().forEachRemaining(k->{if(!allowed.contains(k))throw new ProgramException(400,"Champ non autorisé.");});
    }
    @ExceptionHandler(TrainingAiException.class) public ResponseEntity<?> failure(TrainingAiException ex){
        return ResponseEntity.status(ex.getStatus()).body(new ApiResponse<>(false,ex.getMessage(),Map.of("code",ex.getCode(),"errors",Map.of())));
    }
}
