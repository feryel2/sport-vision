package com.sportvision.training.controller;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.ProgramInput.*;
import com.sportvision.training.entity.ExerciseOptions.ArchiveStatus;
import com.sportvision.training.service.TrainingProgramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/programs")
public class TrainingProgramController {
    private final TrainingProgramService service;
    public TrainingProgramController(TrainingProgramService service){this.service=service;}
    @GetMapping public ApiResponse<PageOutput<ProgramOutput.Summary>> list(@RequestParam(required=false) String search,
        @RequestParam(defaultValue="ACTIVE") ArchiveStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="9") int size){
        return ApiResponse.ok("Programmes chargés.",service.list(search,status,page,size));
    }
    @GetMapping("/{id}") public ApiResponse<ProgramOutput> get(@PathVariable long id){return ApiResponse.ok("Programme chargé.",service.get(id));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<ProgramOutput> create(@Valid @RequestBody ProgramInput input){return ApiResponse.ok("Programme créé.",service.create(input));}
    @PutMapping("/{id}") public ApiResponse<ProgramOutput> update(@PathVariable long id,@Valid @RequestBody Update input){return ApiResponse.ok("Programme enregistré.",service.update(id,input));}
    @PostMapping("/{id}/duplicate") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<ProgramOutput> duplicate(@PathVariable long id){return ApiResponse.ok("Programme dupliqué.",service.duplicate(id));}
    @PatchMapping("/{id}/archive") public ApiResponse<ProgramOutput> archive(@PathVariable long id,@Valid @RequestBody RevisionRequest input){return ApiResponse.ok("Programme archivé.",service.archive(id,input.expectedRevision(),true));}
    @PatchMapping("/{id}/restore") public ApiResponse<ProgramOutput> restore(@PathVariable long id,@Valid @RequestBody RevisionRequest input){return ApiResponse.ok("Programme réactivé.",service.archive(id,input.expectedRevision(),false));}
    @GetMapping("/{id}/revisions") public ApiResponse<PageOutput<ProgramOutput.RevisionSummary>> revisions(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok("Historique chargé.",service.revisions(id,page,size));}
    @GetMapping("/{id}/revisions/{revision}") public ApiResponse<ProgramOutput> revision(@PathVariable long id,@PathVariable int revision){return ApiResponse.ok("Révision chargée.",service.getRevision(id,revision));}
}
