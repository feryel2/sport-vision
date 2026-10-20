package com.sportvision.training.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.sportvision.training.dto.ApiResponse;
import com.sportvision.training.service.ExerciseNotFoundException;

@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = TrainingProgramController.class)
public class ApiExceptionHandler {
    private ResponseEntity<ApiResponse<Map<String, Object>>> error(int code, String message, Map<String, String> fields) {
        return ResponseEntity.status(code).body(new ApiResponse<>(false, message, Map.of("errors", fields)));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return error(400, "Vérifiez les champs du formulaire.", fields);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> malformed(Exception ex) { return error(400, "Format ou valeur invalide. Utilisez les valeurs proposées.", Map.of()); }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> missingParameter(MissingServletRequestParameterException ex) {
        return error(400, "Un paramètre obligatoire est manquant.", Map.of(ex.getParameterName(), "Ce paramètre est obligatoire."));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException ex) { return error(400, ex.getMessage(), Map.of()); }
    @ExceptionHandler(ExerciseNotFoundException.class)
    public ResponseEntity<?> missing(ExerciseNotFoundException ex) { return error(404, "Exercice introuvable.", Map.of()); }
    @ExceptionHandler(com.sportvision.training.service.ProgramException.class)
    public ResponseEntity<?> program(com.sportvision.training.service.ProgramException ex) { return error(ex.getStatus(), ex.getMessage(), Map.of()); }
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<?> concurrent(Exception ex) { return error(409, "Une modification est déjà en cours. Rechargez le programme puis réessayez.", Map.of()); }
    @ExceptionHandler(com.sportvision.training.service.WorkoutException.class)
    public ResponseEntity<?> workout(com.sportvision.training.service.WorkoutException ex) {
        Map<String,Object> data=new LinkedHashMap<>();data.put("errors",Map.of());data.put("code",ex.getCode());
        if(ex.getWorkoutId()!=null)data.put("workoutId",ex.getWorkoutId());data.put("pendingSets",ex.getPendingSets());
        return ResponseEntity.status(ex.getStatus()).body(new ApiResponse<>(false,ex.getMessage(),data));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex) {
        LoggerFactory.getLogger(ApiExceptionHandler.class).error("Échec de la requête training", ex);
        return error(500, "Le service est temporairement indisponible. Réessayez plus tard.", Map.of());
    }
}


