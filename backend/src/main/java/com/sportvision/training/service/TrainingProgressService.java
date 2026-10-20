package com.sportvision.training.service;
import com.sportvision.training.dto.ProgressOutput.*;
import com.sportvision.training.entity.*;
import com.sportvision.training.entity.ExerciseOptions.MeasurementType;
import com.sportvision.training.entity.WorkoutStatuses.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.TrainingProgressRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class TrainingProgressService {
    private static final ZoneId ZONE=ZoneId.of("Africa/Tunis");
    private final TrainingProgressRepository repository;
    private final ObjectProvider<TrainingCurrentUserProvider> users;
    private final Clock clock;
    public TrainingProgressService(TrainingProgressRepository repository,ObjectProvider<TrainingCurrentUserProvider> users,Clock clock){
        this.repository=repository;this.users=users;this.clock=clock;
    }
    private String owner(){
        var provider=users.getIfAvailable();String owner=provider==null?null:provider.ownerReference();
        if(owner==null||owner.isBlank())throw new ProgramException(403,"Identité training indisponible. Le mode démonstration nécessite le profil local.");
        return owner;
    }
    private record Key(long id,MeasurementType type) {}
    private static class Totals {
        final LinkedHashSet<String> names=new LinkedHashSet<>();long count,reps,seconds;BigDecimal load;
        void add(TrainingWorkoutExercise e){
            for(var s:e.getSets())if(s.getStatus()==SetStatus.DONE){
                names.add(e.getName());count++;
                if(e.getMeasurementType()==MeasurementType.REPETITIONS)reps+=s.getRepetitions();else seconds+=s.getDurationSeconds();
                if(s.getLoadKg()!=null&&(load==null||s.getLoadKg().compareTo(load)>0))load=s.getLoadKg();
            }
        }
    }
    public Dashboard dashboard(int days){
        if(days!=7&&days!=30&&days!=90)throw new IllegalArgumentException("Choisissez une période de 7, 30 ou 90 jours.");
        String owner=owner();Instant until=clock.instant();LocalDate today=until.atZone(ZONE).toLocalDate(),first=today.minusDays(days-1);
        Instant from=first.atStartOfDay(ZONE).toInstant();
        var period=new com.sportvision.training.dto.ProgressOutput.Period(days,ZONE.getId(),first,today,from,until);
        var workouts=repository.closedWorkouts(owner,from,until,Set.of(WorkoutStatus.COMPLETED,WorkoutStatus.ABANDONED));
        repository.completedExercises(owner,from,until,WorkoutStatus.COMPLETED);
        Map<LocalDate,Long> daily=new LinkedHashMap<>();
        for(int i=0;i<days;i++)daily.put(first.plusDays(i),0L);
        Map<Key,List<Point>> points=new LinkedHashMap<>();Map<Key,String> latestNames=new HashMap<>();
        long completed=0,abandoned=0,done=0,elapsed=0;
        for(var w:workouts){
            if(w.getStatus()==WorkoutStatus.ABANDONED){abandoned++;continue;}
            completed++;LocalDate date=w.getEndedAt().atZone(ZONE).toLocalDate();daily.merge(date,1L,Long::sum);
            elapsed+=Math.max(0,Duration.between(w.getStartedAt(),w.getEndedAt()).getSeconds());
            // The same source exercise may appear more than once in a session: one point per id/type/workout.
            Map<Key,Totals> session=new LinkedHashMap<>();
            for(var e:w.getExercises())session.computeIfAbsent(new Key(e.getSourceExerciseId(),e.getMeasurementType()),k->new Totals()).add(e);
            for(var entry:session.entrySet()){
                Key key=entry.getKey();Totals t=entry.getValue();if(t.count==0)continue;
                done+=t.count;var names=List.copyOf(t.names);latestNames.put(key,names.get(names.size()-1));
                points.computeIfAbsent(key,k->new ArrayList<>()).add(new Point(w.getId(),w.getEndedAt(),date,w.getSessionName(),names,
                    t.count,key.type()==MeasurementType.REPETITIONS?t.reps:null,key.type()==MeasurementType.DURATION?t.seconds:null,t.load));
            }
        }
        var exercises=points.entrySet().stream().map(e->new Exercise(e.getKey().id(),e.getKey().type(),latestNames.get(e.getKey()),List.copyOf(e.getValue())))
            .sorted(Comparator.comparing(Exercise::name,String.CASE_INSENSITIVE_ORDER).thenComparingLong(Exercise::exerciseId).thenComparing(Exercise::measurementType)).toList();
        return new Dashboard(period,new Indicators(completed,abandoned,done,elapsed),daily.entrySet().stream().map(e->new Day(e.getKey(),e.getValue())).toList(),exercises);
    }
    public Exercise exercise(long id,MeasurementType type,int days){
        if(id<=0)throw new IllegalArgumentException("Identifiant d’exercice invalide.");
        return dashboard(days).exercises().stream().filter(e->e.exerciseId()==id&&e.measurementType()==type).findFirst()
            .orElseThrow(()->new ProgramException(404,"Aucune série réalisée pour cet exercice et ce type de mesure dans la période."));
    }
}

