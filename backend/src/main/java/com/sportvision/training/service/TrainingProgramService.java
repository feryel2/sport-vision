package com.sportvision.training.service;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.ProgramInput.*;
import com.sportvision.training.entity.*;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class TrainingProgramService {
    private final TrainingProgramRepository programs;
    private final TrainingProgramVersionRepository versions;
    private final TrainingExerciseRepository exercises;
    private final ObjectProvider<TrainingCurrentUserProvider> users;
    public TrainingProgramService(TrainingProgramRepository programs,TrainingProgramVersionRepository versions,
        TrainingExerciseRepository exercises,ObjectProvider<TrainingCurrentUserProvider> users){this.programs=programs;this.versions=versions;this.exercises=exercises;this.users=users;}
    private String owner(){
        var provider=users.getIfAvailable();
        String owner=provider==null?null:provider.ownerReference();
        if(owner==null || owner.isBlank()) throw new ProgramException(403,"Identité training indisponible. Le mode démonstration nécessite le profil local.");
        return owner;
    }
    private ProgramException missing(){return new ProgramException(404,"Programme ou révision introuvable.");}
    private TrainingProgram owned(long id,boolean lock){return (lock?programs.lockOwned(id,owner()):programs.findByIdAndOwnerReference(id,owner())).orElseThrow(this::missing);}
    private TrainingProgramVersion current(TrainingProgram p){return revision(p.getId(),p.getCurrentRevision());}
    private TrainingProgramVersion revision(long id,int revision){return versions.findByProgramIdAndRevision(id,revision).orElseThrow(this::missing);}
    private void expected(TrainingProgram p,int expected){if(p.getCurrentRevision()!=expected) throw new ProgramException(409,"Ce programme a changé. Consultez la version courante avant de reprendre vos modifications.");}
    private void pageBounds(int page,int size){if(page<0 || page>100000 || size<1 || size>50) throw new IllegalArgumentException("Pagination invalide : taille de 1 à 50, page de 0 à 100000.");}
    public PageOutput<ProgramOutput.Summary> list(String search,ArchiveStatus status,int page,int size){
        String owner=owner(); pageBounds(page,size);
        if(search!=null && search.length()>120) throw new IllegalArgumentException("Recherche limitée à 120 caractères.");
        Specification<TrainingProgram> spec=(r,q,cb)->cb.equal(r.get("ownerReference"),owner);
        if(status!=ArchiveStatus.ALL) spec=spec.and((r,q,cb)->cb.equal(r.get("archived"),status==ArchiveStatus.ARCHIVED));
        if(search!=null && !search.isBlank()){
            String pattern="%"+search.strip().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
            spec=spec.and((r,q,cb)->cb.like(cb.lower(r.get("name")),pattern,'\\'));
        }
        return PageOutput.from(programs.findAll(spec,PageRequest.of(page,size,Sort.by("name","id"))).map(ProgramOutput.Summary::from));
    }
    public ProgramOutput get(long id){var p=owned(id,false);return ProgramOutput.from(p,current(p));}
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    ProgramOutput lockForReplacement(long id){var p=owned(id,true);return ProgramOutput.from(p,current(p));}
    public PageOutput<ProgramOutput.RevisionSummary> revisions(long id,int page,int size){
        owned(id,false);pageBounds(page,size);
        return PageOutput.from(versions.findByProgramId(id,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"revision"))).map(ProgramOutput.RevisionSummary::from));
    }
    public ProgramOutput getRevision(long id,int number){var p=owned(id,false);return ProgramOutput.from(p,revision(id,number));}
    @Transactional public ProgramOutput create(ProgramInput input){
        String owner=owner(); var draft=resolve(input,null);
        return append(new TrainingProgram(owner),draft,false);
    }
    @Transactional public ProgramOutput update(long id,Update input){
        var p=owned(id,true);expected(p,input.expectedRevision());
        if(p.isArchived()) throw new ProgramException(409,"Réactivez ce programme avant de le modifier.");
        var previous=current(p);var draft=resolve(input.program(),previous);
        if(draft.equals(snapshot(previous))) return ProgramOutput.from(p,previous);
        return append(p,draft,false);
    }
    @Transactional public ProgramOutput duplicate(long id){
        var source=owned(id,true); var draft=snapshot(current(source));
        String name=draft.name().substring(0,Math.min(112,draft.name().length()))+" (copie)";
        return append(new TrainingProgram(source.getOwnerReference()),new Draft(name,draft.description(),draft.objective(),draft.sessions()),false);
    }
    @Transactional public ProgramOutput archive(long id,int expected,boolean archived){
        var p=owned(id,true);expected(p,expected);var v=current(p);
        if(p.isArchived()==archived) return ProgramOutput.from(p,v);
        return append(p,snapshot(v),archived);
    }
    private record Draft(String name,String description,String objective,List<SessionDraft> sessions) {}
    private record SessionDraft(String name,List<PrescriptionDraft> prescriptions) {}
    private record PrescriptionDraft(long exerciseId,String name,Equipment equipment,MeasurementType measurementType,
        int sets,Integer repetitions,Integer durationSeconds,int restSeconds,BigDecimal load) {}
    private BigDecimal load(BigDecimal value){return value==null?null:value.stripTrailingZeros();}
    private Draft snapshot(TrainingProgramVersion v){
        return new Draft(v.getName(),v.getDescription(),v.getObjective(),v.getSessions().stream().map(s->new SessionDraft(s.getName(),
            s.getPrescriptions().stream().map(p->new PrescriptionDraft(p.getExercise().getId(),p.getExerciseName(),p.getEquipment(),p.getMeasurementType(),p.getSets(),p.getRepetitions(),p.getDurationSeconds(),p.getRestSeconds(),load(p.getTargetLoadKg()))).toList())).toList());
    }
    private Draft resolve(ProgramInput input,TrainingProgramVersion previous){
        Map<Long,TrainingPrescription> existing=new HashMap<>();
        if(previous!=null) previous.getSessions().forEach(s->s.getPrescriptions().forEach(p->existing.put(p.getId(),p)));
        Set<Long> reused=new HashSet<>();List<SessionDraft> sessions=new ArrayList<>();
        for(var s:input.sessions()){
            List<PrescriptionDraft> prescriptions=new ArrayList<>();
            for(var p:s.prescriptions()){
                String name; Equipment equipment; MeasurementType type;
                if(p.existingPrescriptionId()!=null){
                    var old=existing.get(p.existingPrescriptionId());
                    if(old==null || !old.getExercise().getId().equals(p.exerciseId()) || !reused.add(old.getId()))
                        throw new IllegalArgumentException("Référence de prescription invalide pour la version courante.");
                    name=old.getExerciseName();equipment=old.getEquipment();type=old.getMeasurementType();
                }else{
                    var exercise=exercises.findById(p.exerciseId()).orElseThrow(()->new IllegalArgumentException("Un exercice sélectionné n’existe plus."));
                    if(exercise.isArchived()) throw new IllegalArgumentException("Un exercice archivé ne peut pas être ajouté. Retirez-le de la nouvelle sélection.");
                    name=exercise.getName();equipment=exercise.getEquipment();type=exercise.getMeasurementType();
                }
                if((type==MeasurementType.REPETITIONS && (p.repetitions()==null || p.durationSeconds()!=null)) ||
                   (type==MeasurementType.DURATION && (p.durationSeconds()==null || p.repetitions()!=null)))
                    throw new IllegalArgumentException("La prescription doit contenir uniquement des répétitions ou une durée, selon le type de l’exercice.");
                prescriptions.add(new PrescriptionDraft(p.exerciseId(),name,equipment,type,p.sets(),p.repetitions(),p.durationSeconds(),p.restSeconds(),load(p.targetLoadKg())));
            }
            sessions.add(new SessionDraft(s.name().strip(),prescriptions));
        }
        return new Draft(input.name().strip(),input.description().strip(),input.objective().strip(),sessions);
    }
    private ProgramOutput append(TrainingProgram program,Draft draft,boolean archived){
        program.advance(draft.name(),draft.description(),draft.objective(),archived,draft.sessions().size());
        programs.saveAndFlush(program);
        var version=new TrainingProgramVersion(program);
        for(int i=0;i<draft.sessions().size();i++){
            var input=draft.sessions().get(i);var session=new TrainingProgramSession(version,input.name(),i+1);
            version.addSession(session);
            for(int j=0;j<input.prescriptions().size();j++){
                var p=input.prescriptions().get(j);
                session.addPrescription(new TrainingPrescription(session,exercises.getReferenceById(p.exerciseId()),p.name(),p.equipment(),p.measurementType(),j+1,p.sets(),p.repetitions(),p.durationSeconds(),p.restSeconds(),p.load()));
            }
        }
        versions.saveAndFlush(version);
        return ProgramOutput.from(program,version);
    }
}
