package com.sportvision.training.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.sportvision.training.dto.*;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.service.TrainingExerciseService;

@RestController
@RequestMapping("/api/training/exercises")
public class TrainingExerciseController {
    private final TrainingExerciseService service;
    public TrainingExerciseController(TrainingExerciseService service) { this.service = service; }
    @GetMapping
    public ApiResponse<PageOutput<ExerciseOutput>> list(
        @RequestParam(required = false) String search, @RequestParam(required = false) MuscleGroup muscleGroup,
        @RequestParam(required = false) Equipment equipment, @RequestParam(required = false) Difficulty difficulty,
        @RequestParam(required = false) MeasurementType measurementType,
        @RequestParam(defaultValue = "ACTIVE") ArchiveStatus status,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "9") int size,
        @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "asc") String direction) {
        return ApiResponse.ok("Catalogue chargé.", service.list(search, muscleGroup, equipment, difficulty, measurementType, status, page, size, sort, direction));
    }
    @GetMapping("/options") public ApiResponse<OptionsOutput> options() { return ApiResponse.ok("Options disponibles.", OptionsOutput.all()); }
    @GetMapping("/{id}") public ApiResponse<ExerciseOutput> get(@PathVariable long id) { return ApiResponse.ok("Exercice chargé.", service.get(id)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExerciseOutput> create(@Valid @RequestBody ExerciseInput input) { return ApiResponse.ok("Exercice créé.", service.create(input)); }
    @PutMapping("/{id}") public ApiResponse<ExerciseOutput> update(@PathVariable long id, @Valid @RequestBody ExerciseInput input) {
        return ApiResponse.ok("Exercice modifié.", service.update(id, input));
    }
    @PatchMapping("/{id}/archive") public ApiResponse<ExerciseOutput> archive(@PathVariable long id) { return ApiResponse.ok("Exercice archivé.", service.archive(id, true)); }
    @PatchMapping("/{id}/restore") public ApiResponse<ExerciseOutput> restore(@PathVariable long id) { return ApiResponse.ok("Exercice réactivé.", service.archive(id, false)); }
}
