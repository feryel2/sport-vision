package com.sportvision.training.controller;
import com.sportvision.training.dto.*;
import com.sportvision.training.entity.ExerciseOptions.MeasurementType;
import com.sportvision.training.service.TrainingProgressService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/training/progress")
public class TrainingProgressController {
    private final TrainingProgressService service;
    public TrainingProgressController(TrainingProgressService service){this.service=service;}
    @GetMapping public ApiResponse<ProgressOutput.Dashboard> dashboard(@RequestParam(defaultValue="30") int days){
        return ApiResponse.ok("Progression chargée.",service.dashboard(days));
    }
    @GetMapping("/exercises/{id}") public ApiResponse<ProgressOutput.Exercise> exercise(@PathVariable long id,@RequestParam MeasurementType measurementType,@RequestParam(defaultValue="30") int days){
        return ApiResponse.ok("Évolution de l’exercice chargée.",service.exercise(id,measurementType,days));
    }
}
