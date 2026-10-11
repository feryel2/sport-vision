package com.sportvision.training.service;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.TrainingAdaptationDto.*;
import com.sportvision.training.entity.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TrainingAdaptationService {
    private final TrainingProgramService programs;
    private final TrainingWorkoutRepository workouts;
    private final TrainingAdaptationProposalRepository proposals;
    private final ObjectProvider<TrainingCurrentUserProvider> users;
    private final GeminiClient gemini; private final ObjectMapper mapper; private final ObjectReader reader;
    private final TransactionTemplate tx; private final Clock clock;
    public TrainingAdaptationService(TrainingProgramService programs,TrainingWorkoutRepository workouts,
        TrainingAdaptationProposalRepository proposals,ObjectProvider<TrainingCurrentUserProvider> users,
        GeminiClient gemini,ObjectMapper mapper,PlatformTransactionManager manager,Clock clock){
        this.programs=programs;this.workouts=workouts;this.proposals=proposals;this.users=users;this.gemini=gemini;this.mapper=mapper;this.clock=clock;
        tx=new TransactionTemplate(manager);
        reader=mapper.copy().disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).readerFor(Selection.class)
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS,DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT).with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    private String owner(){var user=users.getIfAvailable();if(user==null||user.ownerReference()==null||user.ownerReference().isBlank())throw new ProgramException(403,"Identité indisponible.");return user.ownerReference();}
    private ProgramOutput current(long id,int revision,boolean lock){
        var p=lock?programs.lockForReplacement(id):programs.get(id);
        if(p.archived()||p.currentRevision()!=revision)throw conflict();return p;
    }
    private ProgramException conflict(){return new ProgramException(409,"Le programme ou son historique a changé. Rechargez le bilan.");}
    private TrainingAiException invalid(){return new TrainingAiException(502,"INVALID_ADAPTATION","Réponse IA invalide ou ajustement non autorisé. Programme inchangé.");}
    public Report report(long id,int revision){return tx.execute(s->analyze(current(id,revision,false)));}
    private Report analyze(ProgramOutput program){
        String owner=owner();
        var history=workouts.findAll((root,q,cb)->cb.and(cb.equal(root.get("ownerReference"),owner),cb.equal(root.get("programId"),program.id()),
            cb.equal(root.get("programRevision"),program.currentRevision()),cb.equal(root.get("status"),WorkoutStatuses.WorkoutStatus.COMPLETED)),
            PageRequest.of(0,20,Sort.by(Sort.Order.desc("endedAt"),Sort.Order.desc("id")))).getContent();
        List<Item> items=new ArrayList<>();
        for(var session:program.version().sessions())for(var p:session.prescriptions()){
            int exposures=0,done=0,skipped=0,achieved=0,loads=0;long actual=0,prescribed=0;BigDecimal min=null,sum=BigDecimal.ZERO;
            int target=p.repetitions()!=null?p.repetitions():p.durationSeconds();
            for(var w:history)for(var e:w.getExercises())if(Objects.equals(e.getSourcePrescriptionId(),p.id())){
                if(e.getSets().stream().anyMatch(set->set.getStatus()==WorkoutStatuses.SetStatus.DONE))exposures++;
                for(var set:e.getSets())if(set.getStatus()==WorkoutStatuses.SetStatus.DONE){
                    done++;int value=p.repetitions()!=null?set.getRepetitions():set.getDurationSeconds();actual+=value;prescribed+=target;
                    if(value>=target)achieved++;
                    if(set.getLoadKg()!=null){loads++;sum=sum.add(set.getLoadKg());min=min==null?set.getLoadKg():min.min(set.getLoadKg());}
                }else skipped++;
            }
            var before=new Values(p.sets(),p.repetitions(),p.durationSeconds(),p.restSeconds(),p.targetLoadKg());
            var metrics=new Metrics(exposures,done,skipped,actual,prescribed,achieved,loads,min,loads==0?null:sum.divide(BigDecimal.valueOf(loads),2,RoundingMode.HALF_UP),actual-prescribed);
            items.add(new Item(items.size(),p.id(),session.name(),p.exerciseName(),p.measurementType().name(),before,metrics,options(before,metrics)));
        }
        boolean eligible=items.stream().anyMatch(i->!i.options().isEmpty());
        return new Report(program.currentRevision(),history.size(),eligible,eligible?
            "Bilan des 20 dernières séances terminées au maximum, pour la révision courante. Les écarts ne prouvent pas une progression.":
            "Données insuffisantes ou aucun ajustement prudent disponible : au moins deux séances comparables avec des séries réalisées sont nécessaires par exercice. Aucun appel IA effectué.",items);
    }
    private List<Option> options(Values v,Metrics m){
        List<Option> result=new ArrayList<>();
        if(m.workouts()<2||m.done()<2)return result;
        boolean low=m.skipped()>0||m.actual()<m.prescribed()*0.9;
        if(low){
            if(v.sets()>1)result.add(new Option("REDUCE_SETS","Des séries ont été ignorées ou les prescriptions n’ont pas été atteintes.",new Values(v.sets()-1,v.repetitions(),v.durationSeconds(),v.restSeconds(),v.targetLoadKg())));
            int target=v.repetitions()!=null?v.repetitions():v.durationSeconds();
            int delta=Math.min(v.repetitions()!=null?2:10,Math.max(1,target/10));
            if(target>1)result.add(new Option("REDUCE_TARGET","Écart constaté avec les prescriptions ; réduction limitée de la cible.",new Values(v.sets(),v.repetitions()==null?null:Math.max(1,target-delta),v.durationSeconds()==null?null:Math.max(1,target-delta),v.restSeconds(),v.targetLoadKg())));
            if(v.restSeconds()<86400)result.add(new Option("MORE_REST","Récupération supplémentaire proposée après des prescriptions non réalisées.",new Values(v.sets(),v.repetitions(),v.durationSeconds(),Math.min(86400,v.restSeconds()+15),v.targetLoadKg())));
        }else if(m.achieved()==m.done()){
            // Aucune hausse si une charge prescrite n'est pas documentée et atteinte sur toutes les séries.
            boolean loadMet=v.targetLoadKg()==null||v.targetLoadKg().signum()==0||(m.loadSamples()==m.done()&&m.minimumLoadKg().compareTo(v.targetLoadKg())>=0);
            int target=v.repetitions()!=null?v.repetitions():v.durationSeconds();
            int delta=v.repetitions()!=null?1:Math.min(10,Math.max(1,target/10));
            if(loadMet&&target>=5&&target+delta<=(v.repetitions()!=null?10000:86400))result.add(new Option("INCREASE_TARGET","Les cibles ont été atteintes dans les séances observées ; essai limité, sans progression présumée.",new Values(v.sets(),v.repetitions()==null?null:target+delta,v.durationSeconds()==null?null:target+delta,v.restSeconds(),v.targetLoadKg())));
            if(v.targetLoadKg()!=null&&v.targetLoadKg().signum()>0&&m.loadSamples()==m.done()){
                var next=v.targetLoadKg().add(v.targetLoadKg().multiply(new BigDecimal("0.05")).min(new BigDecimal("2.5"))).setScale(2,RoundingMode.DOWN);
                if(next.compareTo(v.targetLoadKg())>0&&next.compareTo(new BigDecimal("10000"))<=0&&m.minimumLoadKg().compareTo(next)>=0)
                    result.add(new Option("OBSERVED_LOAD","Charge déjà réalisée sur toutes les séries observées ; hausse plafonnée à 5 % et 2,5 kg.",new Values(v.sets(),v.repetitions(),v.durationSeconds(),v.restSeconds(),next)));
            }
        }
        return result;
    }
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException("Sérialisation du bilan impossible.");}}
    private String fingerprint(Report report){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(report).getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private Selection parse(String raw){try{Selection s=reader.readValue(raw);if(s==null||s.adjustments()==null)throw invalid();return s;}catch(Exception ex){throw invalid();}}
    private List<Change> validate(Report report,Selection selection){
        if(selection.adjustments().size()>3)throw invalid();Set<Integer> seen=new HashSet<>();List<Change> changes=new ArrayList<>();
        for(var choice:selection.adjustments()){
            if(choice==null||!seen.add(choice.slot()))throw invalid();
            var item=report.items().stream().filter(i->i.slot()==choice.slot()).findFirst().orElseThrow(this::invalid);
            var option=item.options().stream().filter(o->o.code().equals(choice.option())).findFirst().orElseThrow(this::invalid);
            changes.add(new Change(item.slot(),item.session(),item.exercise(),option.reason(),item.before(),option.after()));
        }return changes;
    }
    public Preview propose(long id,int revision){
        var report=report(id,revision);if(!report.eligible())throw new TrainingAiException(422,"INSUFFICIENT_DATA",report.message());
        // Liste blanche : aucun identifiant de base, nom, objectif, commentaire ou date transmis au modèle.
        var data=report.items().stream().filter(i->!i.options().isEmpty()).map(i->Map.of("slot",i.slot(),"measurementType",i.measurementType(),"prescription",i.before(),"metrics",i.metrics(),
            "allowed",i.options().stream().map(o->Map.of("option",o.code(),"after",o.after())).toList())).toList();
        JsonNode schema;
        try{schema=mapper.readTree("""
            {"type":"object","additionalProperties":false,"required":["adjustments"],"properties":{"adjustments":{"type":"array","maxItems":3,"items":{"type":"object","additionalProperties":false,"required":["slot","option"],"properties":{"slot":{"type":"integer"},"option":{"type":"string"}}}}}}
            """);}catch(Exception ex){throw new IllegalStateException(ex);}
        var selection=parse(gemini.generate("Sélectionne au maximum trois ajustements prudents, un par slot, exclusivement parmi allowed. Retourne uniquement adjustments (slot, option). Une liste vide est autorisée. Les indicateurs sont calculés par Java ; ne déduis aucune progression ni information médicale.",json(data),schema));
        var changes=validate(report,selection);
        return tx.execute(s->{var fresh=analyze(current(id,revision,false));if(!fingerprint(fresh).equals(fingerprint(report)))throw conflict();
            UUID token=changes.isEmpty()?null:proposals.save(new TrainingAdaptationProposal(owner(),id,revision,fingerprint(report),json(selection),clock.instant().plusSeconds(1800))).getId();
            return new Preview(token,report,changes);});
    }
    public ProgramOutput confirm(long id,int revision,UUID token){return tx.execute(s->{
        var program=current(id,revision,true);
        var proposal=proposals.findById(token).filter(p->p.getOwnerReference().equals(owner())&&p.getProgramId()==id)
            .orElseThrow(()->new ProgramException(404,"Proposition introuvable."));
        var report=analyze(program);
        if(proposal.getRevision()!=revision||!proposal.getExpiresAt().isAfter(clock.instant())||!proposal.getFingerprint().equals(fingerprint(report)))throw conflict();
        var changes=validate(report,parse(proposal.getSelection()));if(changes.isEmpty())throw conflict();
        Map<Long,Values> adjusted=new HashMap<>();for(var change:changes)adjusted.put(report.items().get(change.slot()).prescriptionId(),change.after());
        var v=program.version();var sessions=v.sessions().stream().map(session->new ProgramInput.SessionInput(session.name(),session.prescriptions().stream().map(p->{
            var a=adjusted.getOrDefault(p.id(),new Values(p.sets(),p.repetitions(),p.durationSeconds(),p.restSeconds(),p.targetLoadKg()));
            return new ProgramInput.PrescriptionInput(p.id(),p.exerciseId(),a.sets(),a.repetitions(),a.durationSeconds(),a.restSeconds(),a.targetLoadKg());
        }).toList())).toList();
        return programs.update(id,new ProgramInput.Update(revision,new ProgramInput(v.name(),v.description(),v.objective(),sessions)));
    });}
}
