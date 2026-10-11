package com.sportvision.training.service;
import com.sportvision.training.dto.*;
import com.sportvision.training.entity.*;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.entity.WorkoutStatuses.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TrainingWorkoutService {
    private final TrainingWorkoutRepository workouts;private final TrainingProgramRepository programs;
    private final TrainingProgramVersionRepository versions;private final ObjectProvider<TrainingCurrentUserProvider> users;
    private final TransactionTemplate transactions;
    public TrainingWorkoutService(TrainingWorkoutRepository workouts,TrainingProgramRepository programs,TrainingProgramVersionRepository versions,
        ObjectProvider<TrainingCurrentUserProvider> users,PlatformTransactionManager manager){
        this.workouts=workouts;this.programs=programs;this.versions=versions;this.users=users;transactions=new TransactionTemplate(manager);
    }
    private String owner(){
        var provider=users.getIfAvailable();String owner=provider==null?null:provider.ownerReference();
        if(owner==null||owner.isBlank())throw new ProgramException(403,"Identité training indisponible. Le mode démonstration nécessite le profil local.");
        return owner;
    }
    private WorkoutException missing(){return new WorkoutException(404,"Séance ou série introuvable.","NOT_FOUND",null,0);}
    private TrainingWorkout owned(long id,boolean lock){return (lock?workouts.lockOwned(id,owner()):workouts.findByIdAndOwnerReference(id,owner())).orElseThrow(this::missing);}
    private void editable(TrainingWorkout w){if(w.getStatus()!=WorkoutStatus.IN_PROGRESS)throw new WorkoutException(409,"Cette séance est close et ne peut plus être modifiée.","WORKOUT_CLOSED",w.getId(),0);}
    private WorkoutOutput resume(TrainingWorkout active,WorkoutInput.Start input){
        if(active.getProgramId().equals(input.programId())&&active.getProgramRevision()==input.revision()&&active.getSourceSessionId().equals(input.sessionId()))return WorkoutOutput.from(active);
        throw new WorkoutException(409,"Une autre séance est déjà en cours. Reprenez-la avant d’en démarrer une nouvelle.","ACTIVE_WORKOUT_CONFLICT",active.getId(),0);
    }
    // A failed unique insert must finish rolling back before reading the winner in a fresh transaction.
    public WorkoutOutput start(WorkoutInput.Start input){
        String owner=owner();
        for(int attempt=0;attempt<3;attempt++){
            try{return transactions.execute(tx->startInside(owner,input));}
            catch(DataIntegrityViolationException collision){
                WorkoutOutput resumed=transactions.execute(tx->workouts.findByActiveOwner(owner).map(w->resume(w,input)).orElse(null));
                if(resumed!=null)return resumed;
                if(attempt==2)throw collision;
            }
        }
        throw new IllegalStateException("Unreachable start outcome");
    }
    // Réutilisé par la planification dans sa transaction, sans dupliquer le parcours d'exécution.
    WorkoutOutput startInside(String owner,WorkoutInput.Start input){
        var active=workouts.findByActiveOwner(owner);if(active.isPresent())return resume(active.get(),input);
        var program=programs.lockOwned(input.programId(),owner).orElseThrow(this::missing);
        if(program.isArchived())throw new WorkoutException(409,"Réactivez le programme avant de démarrer une séance.","PROGRAM_ARCHIVED",null,0);
        var version=versions.findByProgramIdAndRevision(program.getId(),input.revision()).orElseThrow(this::missing);
        var session=version.getSessions().stream().filter(s->s.getId().equals(input.sessionId())).findFirst().orElseThrow(this::missing);
        // Recheck after the program lock, for two starts from the same program.
        active=workouts.findByActiveOwner(owner);if(active.isPresent())return resume(active.get(),input);
        var workout=new TrainingWorkout(owner,program,version,session);
        session.getPrescriptions().forEach(p->workout.addExercise(new TrainingWorkoutExercise(workout,p)));
        workout.refreshProgress();workouts.saveAndFlush(workout);return WorkoutOutput.from(workout);
    }
    @Transactional(readOnly=true) public WorkoutOutput get(long id){return WorkoutOutput.from(owned(id,false));}
    @Transactional(readOnly=true) public WorkoutOutput.Summary current(){return workouts.findByActiveOwner(owner()).map(WorkoutOutput.Summary::from).orElse(null);}
    @Transactional(readOnly=true) public PageOutput<WorkoutOutput.Summary> history(WorkoutStatus status,int page,int size){
        String owner=owner();if(page<0||page>100000||size<1||size>50)throw new IllegalArgumentException("Pagination invalide : taille de 1 à 50, page de 0 à 100000.");
        Specification<TrainingWorkout> spec=(r,q,cb)->cb.equal(r.get("ownerReference"),owner);
        if(status!=null)spec=spec.and((r,q,cb)->cb.equal(r.get("status"),status));
        return PageOutput.from(workouts.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"startedAt","id"))).map(WorkoutOutput.Summary::from));
    }
    private record Series(TrainingWorkoutExercise exercise,TrainingWorkoutSet set) {}
    private Series series(TrainingWorkout w,long id){
        for(var e:w.getExercises())for(var s:e.getSets())if(s.getId()==id)return new Series(e,s);
        throw missing();
    }
    @Transactional public WorkoutOutput saveSet(long id,long setId,WorkoutInput.SetPerformance input){
        var workout=owned(id,true);editable(workout);var series=series(workout,setId);
        if((series.exercise().getMeasurementType()==MeasurementType.REPETITIONS&&(input.repetitions()==null||input.durationSeconds()!=null))||
           (series.exercise().getMeasurementType()==MeasurementType.DURATION&&(input.durationSeconds()==null||input.repetitions()!=null)))
            throw new IllegalArgumentException("Renseignez uniquement les répétitions ou la durée réellement effectuée, selon le type de l’exercice.");
        series.set().done(input.repetitions(),input.durationSeconds(),input.loadKg());workout.refreshProgress();workouts.flush();return WorkoutOutput.from(workout);
    }
    @Transactional public WorkoutOutput skipSet(long id,long setId){
        var workout=owned(id,true);editable(workout);series(workout,setId).set().skip();workout.refreshProgress();workouts.flush();return WorkoutOutput.from(workout);
    }
    @Transactional public WorkoutOutput complete(long id,WorkoutInput.Complete input){
        var workout=owned(id,true);
        if(workout.getStatus()==WorkoutStatus.COMPLETED)return WorkoutOutput.from(workout);
        editable(workout);workout.refreshProgress();
        if(workout.getDoneSets()==0)throw new WorkoutException(400,"Validez au moins une série réalisée avant de terminer.","NO_DONE_SET",id,0);
        int pending=workout.getTotalSets()-workout.getDoneSets()-workout.getSkippedSets();
        if(pending>0&&!input.confirmSkipPending())throw new WorkoutException(409,"Des séries restent à faire. Confirmez leur classement comme ignorées pour terminer.","PENDING_CONFIRMATION_REQUIRED",id,pending);
        if(pending>0)workout.getExercises().forEach(e->e.getSets().stream().filter(s->s.getStatus()==SetStatus.PENDING).forEach(TrainingWorkoutSet::skip));
        workout.refreshProgress();workout.close(WorkoutStatus.COMPLETED,input.comment());workouts.flush();return WorkoutOutput.from(workout);
    }
    @Transactional public WorkoutOutput abandon(long id,WorkoutInput.Abandon input){
        var workout=owned(id,true);if(workout.getStatus()==WorkoutStatus.ABANDONED)return WorkoutOutput.from(workout);
        editable(workout);workout.close(WorkoutStatus.ABANDONED,input.comment());workouts.flush();return WorkoutOutput.from(workout);
    }
}
