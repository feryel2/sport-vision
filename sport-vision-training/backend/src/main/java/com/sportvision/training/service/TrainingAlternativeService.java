package com.sportvision.training.service;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.TrainingAlternativeDto.*;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.repository.TrainingExerciseRepository;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingAlternativeService {
    private final TrainingProgramService programs;private final TrainingExerciseRepository exercises;
    private final GeminiClient gemini;private final Validator validator;private final ObjectMapper mapper;
    private final ObjectReader reader;private final String prompt;private final JsonNode schema;
    public TrainingAlternativeService(TrainingProgramService programs,TrainingExerciseRepository exercises,
        GeminiClient gemini,Validator validator,ObjectMapper mapper)throws java.io.IOException{
        this.programs=programs;this.exercises=exercises;this.gemini=gemini;this.validator=validator;this.mapper=mapper;
        prompt=new ClassPathResource("training/ai/alternative-prompt.txt").getContentAsString(StandardCharsets.UTF_8);
        schema=mapper.readTree(new ClassPathResource("training/ai/alternative-schema.json").getContentAsString(StandardCharsets.UTF_8));
        reader=mapper.copy().disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).readerFor(Selection.class)
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS).without(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    private record Context(ProgramOutput program,ProgramOutput.Prescription source,TrainingAiDto.CatalogExercise current,List<TrainingAiDto.CatalogExercise> candidates) {}
    // Liste blanche dédiée à Gemini : aucun texte libre, même issu du catalogue.
    private record ExerciseCharacteristics(long id,MuscleGroup muscleGroup,Equipment equipment,MeasurementType measurementType) {
        static ExerciseCharacteristics from(TrainingAiDto.CatalogExercise exercise){
            return new ExerciseCharacteristics(exercise.id(),exercise.muscleGroup(),exercise.equipment(),exercise.measurementType());
        }
    }
    private Context prepare(long programId,Request request,boolean lock){
        if(request==null||!validator.validate(request).isEmpty())throw new IllegalArgumentException("Préférences de remplacement invalides.");
        // get vérifie le propriétaire fourni par le serveur et ne charge que la version courante.
        var program=lock?programs.lockForReplacement(programId):programs.get(programId);
        if(program.currentRevision()!=request.expectedRevision())throw new ProgramException(409,"Ce programme a changé. Rechargez sa version courante.");
        if(program.archived())throw new ProgramException(409,"Un programme archivé ne peut pas être modifié.");
        var session=program.version().sessions().stream().filter(s->s.prescriptions().stream().anyMatch(p->p.id().equals(request.prescriptionId())))
            .findFirst().orElseThrow(()->new ProgramException(404,"Prescription introuvable dans la version courante."));
        var source=session.prescriptions().stream().filter(p->p.id().equals(request.prescriptionId())).findFirst().orElseThrow();
        var current=exercises.findById(source.exerciseId()).orElseThrow(()->new ProgramException(404,"Exercice source introuvable."));
        var used=session.prescriptions().stream().map(ProgramOutput.Prescription::exerciseId).toList();
        Set<Equipment> available=new HashSet<>(request.equipment());available.add(Equipment.NONE);
        var levels=Arrays.stream(Difficulty.values()).filter(v->v.ordinal()<=request.level().ordinal()).toList();
        var candidates=exercises.findAll((root,query,cb)->cb.and(cb.isFalse(root.get("archived")),
            cb.equal(root.get("muscleGroup"),current.getMuscleGroup()),cb.equal(root.get("measurementType"),source.measurementType()),
            root.get("equipment").in(available),root.get("difficulty").in(levels),cb.not(root.get("id").in(used))),
            PageRequest.of(0,40,Sort.by("id"))).stream()
            .map(e->new TrainingAiDto.CatalogExercise(e.getId(),e.getName(),e.getMuscleGroup(),e.getEquipment(),e.getMeasurementType())).toList();
        return new Context(program,source,new TrainingAiDto.CatalogExercise(source.exerciseId(),source.exerciseName(),current.getMuscleGroup(),source.equipment(),source.measurementType()),candidates);
    }
    public Preview propose(long programId,Request request){
        var context=prepare(programId,request,false);
        if(context.candidates().isEmpty())throw new TrainingAiException(422,"NO_ALTERNATIVE","Aucune alternative compatible : même groupe musculaire et même mesure, selon votre matériel et votre niveau. Aucun appel IA effectué.");
        String data;
        try{data=mapper.writeValueAsString(Map.of("current",ExerciseCharacteristics.from(context.current()),
            "motive",request.motive(),"level",request.level(),"equipment",request.equipment(),
            "catalogue",context.candidates().stream().map(ExerciseCharacteristics::from).toList()));}
        catch(Exception ex){throw new TrainingAiException(503,"CONFIGURATION","Préparation de la proposition indisponible.");}
        String raw=gemini.generate(prompt,data,schema);Selection selection;
        try{selection=reader.readValue(raw);}catch(Exception ex){throw invalid();}
        if(selection==null||!validator.validate(selection).isEmpty())throw invalid();
        var alternative=context.candidates().stream().filter(e->e.id()==selection.exerciseId()).findFirst().orElseThrow(this::invalid);
        return new Preview(request,context.source(),alternative,selection.reason().strip());
    }
    private TrainingAiException invalid(){return new TrainingAiException(502,"INVALID_ALTERNATIVE","La réponse IA ne désigne pas une alternative autorisée avec une explication valide. Programme inchangé.");}
    @Transactional public ProgramOutput confirm(long programId,Confirmation confirmation){
        if(confirmation==null||!validator.validate(confirmation).isEmpty())throw new IllegalArgumentException("Confirmation invalide.");
        var context=prepare(programId,confirmation.request(),true);
        if(context.candidates().stream().noneMatch(e->e.id()==confirmation.exerciseId()))
            throw new TrainingAiException(400,"INVALID_ALTERNATIVE","Cette alternative n’est plus compatible. Programme inchangé.");
        var version=context.program().version();
        var sessions=version.sessions().stream().map(s->new ProgramInput.SessionInput(s.name(),s.prescriptions().stream().map(p->{
            boolean target=p.id().equals(confirmation.request().prescriptionId());
            return new ProgramInput.PrescriptionInput(target?null:p.id(),target?confirmation.exerciseId():p.exerciseId(),
                p.sets(),p.repetitions(),p.durationSeconds(),p.restSeconds(),target?null:p.targetLoadKg());
        }).toList())).toList();
        // update verrouille le programme et revérifie expectedRevision avant de créer une seule révision.
        return programs.update(programId,new ProgramInput.Update(confirmation.request().expectedRevision(),
            new ProgramInput(version.name(),version.description(),version.objective(),sessions)));
    }
}
