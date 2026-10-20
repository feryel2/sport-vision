package com.sportvision.training.service;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.TrainingAiDto.*;
import com.sportvision.training.entity.TrainingExercise;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.TrainingExerciseRepository;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TrainingAiService {
    private final TrainingExerciseRepository exercises;private final TrainingProgramService programs;
    private final GeminiClient gemini;private final Validator validator;private final ObjectMapper mapper;
    private final ObjectReader proposalReader;private final ObjectProvider<TrainingCurrentUserProvider> users;
    private final String prompt;private final JsonNode schema;
    private final TrainingAiConfirmationService confirmations;
    public TrainingAiService(TrainingExerciseRepository exercises,TrainingProgramService programs,GeminiClient gemini,
        Validator validator,ObjectMapper mapper,ObjectProvider<TrainingCurrentUserProvider> users,TrainingAiConfirmationService confirmations)throws java.io.IOException{
        this.confirmations=confirmations;
        this.exercises=exercises;this.programs=programs;this.gemini=gemini;this.validator=validator;this.mapper=mapper;this.users=users;
        prompt=new ClassPathResource("training/ai/program-prompt.txt").getContentAsString(StandardCharsets.UTF_8);
        schema=mapper.readTree(new ClassPathResource("training/ai/program-schema.json").getContentAsString(StandardCharsets.UTF_8));
        proposalReader=mapper.copy().disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).readerFor(Proposal.class)
            // Seules les mesures optionnelles peuvent manquer ; Bean Validation exige tous les autres champs.
            .without(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS).with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    public Options options(){
        return new Options(Arrays.stream(Goal.values()).map(v->new Option(v.name(),v.label())).toList(),
            Arrays.stream(Difficulty.values()).map(v->new Option(v.name(),v.label())).toList(),
            Arrays.stream(Equipment.values()).map(v->new Option(v.name(),v.label())).toList());
    }
    public Preview generate(Preferences preferences){
        requireOwner();validatePreferences(preferences);gemini.requireConfigured();
        var catalogue=compatibleCatalogue(preferences);
        if(catalogue.size()<3)throw new TrainingAiException(422,"CATALOGUE_INSUFFICIENT","Il faut au moins trois exercices actifs compatibles. Modifiez le matériel ou créez le programme manuellement.");
        String raw=callGemini(preferences,catalogue);
        Proposal proposal;
        try{proposal=proposalReader.readValue(raw);}
        catch(Exception ex){throw invalid("Réponse IA JSON invalide ou incomplète.");}
        validateProposal(preferences,proposal,catalogue,502);
        return new Preview(proposal,catalogue);
    }
    private String callGemini(Preferences preferences,List<CatalogExercise> catalogue){
        try{
            // Aucun propriétaire ni historique : seulement les choix et cinq champs du catalogue.
            String data=mapper.writeValueAsString(Map.of("preferences",preferences,"catalogue",catalogue));
            return gemini.generate(prompt,data,schema);
        }catch(TrainingAiException ex){throw ex;}
        catch(Exception ex){throw new TrainingAiException(503,"UNAVAILABLE","Service IA indisponible.");}
    }
    public ProgramOutput save(Save request){
        String owner=requireOwner();
        if(request==null||!validator.validate(request).isEmpty())
            throw new TrainingAiException(400,"INVALID_INPUT","Clé de confirmation ou proposition invalide.");
        String hash=requestHash(request);
        return confirmations.confirm(owner,request.idempotencyKey(),hash,()->{
            // Le catalogue n'est revérifié que pour une nouvelle création : une répétition
            // doit retrouver son programme même si un exercice a été archivé depuis.
            var catalogue=compatibleCatalogue(request.preferences());
            ProgramInput program=validateProposal(request.preferences(),request.proposal(),catalogue,400);
            return programs.create(program);
        });
    }
    private String requestHash(Save request){
        try {
            var content=mapper.createObjectNode();
            var preferences=mapper.valueToTree(request.preferences());
            var equipment=mapper.createArrayNode();
            request.preferences().equipment().stream().map(Enum::name).sorted().forEach(equipment::add);
            ((com.fasterxml.jackson.databind.node.ObjectNode)preferences).set("equipment",equipment);
            content.set("preferences",preferences);
            content.set("proposal",mapper.valueToTree(request.proposal()));
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(content)));
        } catch(Exception ex){throw new IllegalStateException("Empreinte de confirmation indisponible.",ex);}
    }
    private String requireOwner(){
        var provider=users.getIfAvailable();
        String owner=provider==null?null:provider.ownerReference();
        if(owner==null||owner.isBlank())
            throw new ProgramException(403,"Identité training indisponible. Le mode démonstration nécessite le profil local.");
        return owner;
    }
    private void validatePreferences(Preferences preferences){
        if(preferences==null||!validator.validate(preferences).isEmpty())throw new TrainingAiException(400,"INVALID_INPUT","Vérifiez l’objectif, le niveau, le nombre de séances et le matériel.");
    }
    private List<CatalogExercise> compatibleCatalogue(Preferences preferences){
        Set<Equipment> available=new HashSet<>(preferences.equipment());available.add(Equipment.NONE);
        var levels=Arrays.stream(Difficulty.values()).filter(v->v.ordinal()<=preferences.level().ordinal()).toList();
        Specification<TrainingExercise> spec=(r,q,cb)->cb.and(cb.isFalse(r.get("archived")),r.get("equipment").in(available),r.get("difficulty").in(levels));
        return exercises.findAll(spec,PageRequest.of(0,40,Sort.by("id"))).stream()
            .map(e->new CatalogExercise(e.getId(),e.getName(),e.getMuscleGroup(),e.getEquipment(),e.getMeasurementType())).toList();
    }
    private ProgramInput validateProposal(Preferences preferences,Proposal proposal,List<CatalogExercise> catalogue,int status){
        if(proposal==null||!validator.validate(proposal).isEmpty()||proposal.sessions().size()!=preferences.sessionCount())
            throw rejected(status,"Proposition incomplète, textes trop longs ou nombre de séances incorrect.");
        Map<Long,CatalogExercise> allowed=new HashMap<>();catalogue.forEach(e->allowed.put(e.id(),e));
        List<ProgramInput.SessionInput> sessions=new ArrayList<>();
        for(var session:proposal.sessions()){
            Set<Long> used=new HashSet<>();List<ProgramInput.PrescriptionInput> prescriptions=new ArrayList<>();
            for(var choice:session.exercises()){
                var exercise=allowed.get(choice.exerciseId());
                if(exercise==null||!used.add(choice.exerciseId()))
                    throw rejected(status,"La proposition contient un exercice non autorisé ou répété dans une séance.");
                if(exercise.measurementType()!=choice.measurementType()||
                    (exercise.measurementType()==MeasurementType.REPETITIONS&&(choice.repetitions()==null||choice.durationSeconds()!=null))||
                    (exercise.measurementType()==MeasurementType.DURATION&&(choice.durationSeconds()==null||choice.repetitions()!=null)))
                    throw rejected(status,"Une prescription ne respecte pas le type de mesure du catalogue.");
                prescriptions.add(new ProgramInput.PrescriptionInput(null,exercise.id(),choice.sets(),choice.repetitions(),choice.durationSeconds(),choice.restSeconds(),null));
            }
            sessions.add(new ProgramInput.SessionInput(session.name(),prescriptions));
        }
        var program=new ProgramInput(proposal.name(),proposal.description(),preferences.objective().label(),sessions);
        if(!validator.validate(program).isEmpty())throw rejected(status,"La proposition dépasse les limites métier.");
        return program;
    }
    private TrainingAiException invalid(String message){return rejected(502,message);}
    private TrainingAiException rejected(int status,String message){return new TrainingAiException(status,"INVALID_PROPOSAL",message+" Aucun programme enregistré.");}
}
