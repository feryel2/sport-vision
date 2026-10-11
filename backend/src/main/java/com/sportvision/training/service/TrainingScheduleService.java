package com.sportvision.training.service;
import com.sportvision.training.dto.*;
import com.sportvision.training.dto.ScheduleDto.*;
import com.sportvision.training.entity.TrainingSchedule;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
@Service
public class TrainingScheduleService {
    private static final ZoneId ZONE=ZoneId.of("Africa/Tunis");
    private final TrainingScheduleRepository schedules;private final TrainingProgramService programs;
    private final TrainingWorkoutService workouts;private final TrainingWorkoutRepository workoutRepository;
    private final ObjectProvider<TrainingCurrentUserProvider> users;private final Clock clock;private final TransactionTemplate tx;
    public TrainingScheduleService(TrainingScheduleRepository schedules,TrainingProgramService programs,TrainingWorkoutService workouts,
        TrainingWorkoutRepository workoutRepository,ObjectProvider<TrainingCurrentUserProvider> users,Clock clock,PlatformTransactionManager manager){
        this.schedules=schedules;this.programs=programs;this.workouts=workouts;this.workoutRepository=workoutRepository;this.users=users;this.clock=clock;tx=new TransactionTemplate(manager);
    }
    private String owner(){var p=users.getIfAvailable();String owner=p==null?null:p.ownerReference();if(owner==null||owner.isBlank())throw new ProgramException(403,"Identité training indisponible.");return owner;}
    private TrainingSchedule owned(long id){return schedules.lockOwned(id,owner()).orElseThrow(()->new ProgramException(404,"Planification introuvable."));}
    private void expected(TrainingSchedule s,long version){if(s.getVersion()!=version)throw new ProgramException(409,"La planification a changé. Rechargez la liste.");}
    private void editable(TrainingSchedule s){if(s.isCancelled()||s.getWorkout()!=null)throw new ProgramException(409,"Une planification annulée ou déjà démarrée ne peut plus être modifiée.");}
    private Instant time(LocalDateTime local){
        if(local==null||local.getSecond()!=0||local.getNano()!=0||ZONE.getRules().getValidOffsets(local).size()!=1)
            throw new ProgramException(400,"Choisissez une date et une heure à la minute, dans le fuseau Africa/Tunis.");
        Instant instant=local.atZone(ZONE).toInstant();
        if(!instant.isAfter(clock.instant()))throw new ProgramException(400,"La date planifiée doit être dans le futur (Africa/Tunis).");
        return instant;
    }
    private <T>T write(Supplier<T> action){
        try{return tx.execute(s->action.get());}
        catch(DataIntegrityViolationException ex){throw new ProgramException(409,"Ce créneau ou cet entraînement est déjà utilisé. Rechargez la planification.");}
    }
    private Output output(TrainingSchedule s){var w=s.getWorkout();String status=s.isCancelled()?"CANCELLED":w==null?"PLANNED":w.getStatus().name();
        return new Output(s.getId(),s.getVersion(),s.getProgramId(),s.getProgramRevision(),s.getSessionId(),s.getProgramName(),s.getSessionName(),s.getScheduledAt(),LocalDateTime.ofInstant(s.getScheduledAt(),ZONE),ZONE.getId(),status,w==null?null:w.getId());}
    public Output create(Create input){return write(()->{
        Instant date=time(input.scheduledLocal());var p=programs.lockForReplacement(input.programId());
        if(p.archived()||p.currentRevision()!=input.expectedRevision())throw new ProgramException(409,"Le programme est archivé ou sa révision a changé. Rechargez-le.");
        var session=p.version().sessions().stream().filter(s->s.id().equals(input.sessionId())).findFirst().orElseThrow(()->new ProgramException(400,"Cette séance n’appartient pas à la révision du programme."));
        return output(schedules.saveAndFlush(new TrainingSchedule(owner(),p.id(),p.currentRevision(),session.id(),p.name(),session.name(),date)));
    });}
    @Transactional(readOnly=true) public PageOutput<Output> list(LocalDate from,LocalDate to,int page,int size){
        if(from==null||to==null||to.isBefore(from)||ChronoUnit.DAYS.between(from,to)>92||page<0||page>100000||size<1||size>50)
            throw new ProgramException(400,"Choisissez une période de 1 à 93 jours et une pagination valide.");
        String owner=owner();Instant begin=from.atStartOfDay(ZONE).toInstant(),end=to.plusDays(1).atStartOfDay(ZONE).toInstant();
        return PageOutput.from(schedules.findAll((r,q,cb)->cb.and(cb.equal(r.get("ownerReference"),owner),cb.greaterThanOrEqualTo(r.get("scheduledAt"),begin),cb.lessThan(r.get("scheduledAt"),end)),
            PageRequest.of(page,size,Sort.by("scheduledAt","id"))).map(this::output));
    }
    public Output update(long id,Update input){return write(()->{
        var s=owned(id);expected(s,input.expectedVersion());editable(s);
        if(programs.lockForReplacement(s.getProgramId()).archived())throw new ProgramException(409,"Réactivez le programme avant de replanifier.");
        s.reschedule(time(input.scheduledLocal()));schedules.flush();return output(s);
    });}
    public Output cancel(long id,Action input){return write(()->{
        var s=owned(id);if(s.isCancelled())return output(s);expected(s,input.expectedVersion());editable(s);s.cancel();schedules.flush();return output(s);
    });}
    public WorkoutOutput start(long id,Action input){return write(()->{
        var s=owned(id);
        // Une répétition renvoie toujours le même entraînement, même après sa clôture.
        if(s.getWorkout()!=null)return workouts.get(s.getWorkout().getId());
        expected(s,input.expectedVersion());editable(s);
        if(programs.lockForReplacement(s.getProgramId()).archived())throw new ProgramException(409,"Réactivez le programme avant de démarrer cette planification.");
        // Même transaction : création/reprise existante et liaison sont atomiques.
        var w=workouts.startInside(owner(),new WorkoutInput.Start(s.getProgramId(),s.getProgramRevision(),s.getSessionId()));
        if(schedules.existsByWorkoutId(w.id()))throw new ProgramException(409,"Cette séance en cours est déjà liée à une autre planification.");
        s.attach(workoutRepository.getReferenceById(w.id()));schedules.flush();return w;
    });}
}
