package com.sportvision.training.controller;
import com.sportvision.training.dto.*;
import com.sportvision.training.entity.WorkoutStatuses.WorkoutStatus;
import com.sportvision.training.service.TrainingWorkoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/training/workouts")
public class TrainingWorkoutController {
    private final TrainingWorkoutService service;
    public TrainingWorkoutController(TrainingWorkoutService service){this.service=service;}
    @PostMapping public ApiResponse<WorkoutOutput> start(@Valid @RequestBody WorkoutInput.Start input){return ApiResponse.ok("Séance démarrée ou reprise.",service.start(input));}
    @GetMapping("/current") public ApiResponse<WorkoutOutput.Summary> current(){return ApiResponse.ok("Séance en cours chargée.",service.current());}
    @GetMapping public ApiResponse<PageOutput<WorkoutOutput.Summary>> history(@RequestParam(required=false) WorkoutStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ApiResponse.ok("Historique chargé.",service.history(status,page,size));}
    @GetMapping("/{id}") public ApiResponse<WorkoutOutput> get(@PathVariable long id){return ApiResponse.ok("Séance chargée.",service.get(id));}
    @PutMapping("/{id}/sets/{setId}") public ApiResponse<WorkoutOutput> save(@PathVariable long id,@PathVariable long setId,@Valid @RequestBody WorkoutInput.SetPerformance input){return ApiResponse.ok("Série enregistrée.",service.saveSet(id,setId,input));}
    @PatchMapping("/{id}/sets/{setId}/skip") public ApiResponse<WorkoutOutput> skip(@PathVariable long id,@PathVariable long setId){return ApiResponse.ok("Série ignorée.",service.skipSet(id,setId));}
    @PostMapping("/{id}/complete") public ApiResponse<WorkoutOutput> complete(@PathVariable long id,@Valid @RequestBody WorkoutInput.Complete input){return ApiResponse.ok("Séance terminée.",service.complete(id,input));}
    @PostMapping("/{id}/abandon") public ApiResponse<WorkoutOutput> abandon(@PathVariable long id,@Valid @RequestBody WorkoutInput.Abandon input){return ApiResponse.ok("Séance abandonnée.",service.abandon(id,input));}
}
