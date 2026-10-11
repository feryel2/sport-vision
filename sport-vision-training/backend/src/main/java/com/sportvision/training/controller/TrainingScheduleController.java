package com.sportvision.training.controller;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.ScheduleDto.*;
import com.sportvision.training.service.TrainingScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/schedules")
public class TrainingScheduleController {
    private final TrainingScheduleService service;
    public TrainingScheduleController(TrainingScheduleService service){this.service=service;}
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> missingDate(){return new ApiResponse<>(false,"Les dates de début et de fin sont obligatoires.",null);}
    @GetMapping public ApiResponse<PageOutput<Output>> list(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        return ApiResponse.ok("Planification chargée.",service.list(from,to,page,size));
    }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<Output> create(@Valid @RequestBody Create input){return ApiResponse.ok("Séance planifiée.",service.create(input));}
    @PutMapping("/{id}") public ApiResponse<Output> update(@PathVariable long id,@Valid @RequestBody Update input){return ApiResponse.ok("Date modifiée.",service.update(id,input));}
    @PostMapping("/{id}/cancel") public ApiResponse<Output> cancel(@PathVariable long id,@Valid @RequestBody Action input){return ApiResponse.ok("Planification annulée.",service.cancel(id,input));}
    @PostMapping("/{id}/start") public ApiResponse<WorkoutOutput> start(@PathVariable long id,@Valid @RequestBody Action input){return ApiResponse.ok("Entraînement chargé.",service.start(id,input));}
}
