package com.sportvision.training.bootstrap;

import com.sportvision.training.dto.*;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** Local demonstration only. Seeds an empty H2 database atomically, once. */
@Component
@Profile("local-demo")
@ConditionalOnProperty(name="training.dataset.enabled",havingValue="true")
public class TrainingLocalDatasetInitializer implements ApplicationRunner {
 private static final Logger LOG=LoggerFactory.getLogger(TrainingLocalDatasetInitializer.class);
 private static final String MARK="integration-populated-v1";
 private static final ZoneId ZONE=ZoneId.of("Africa/Tunis");
 private final ApplicationContext ctx;
 public TrainingLocalDatasetInitializer(ApplicationContext ctx){this.ctx=ctx;}
 private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
 private static void valid(Validator v,Object x){require(v.validate(x).isEmpty(),"Fixture failed Bean Validation");}
 @Override public void run(ApplicationArguments args)throws Exception{
  try(var connection=ctx.getBean(javax.sql.DataSource.class).getConnection()){
   String url=connection.getMetaData().getURL();
   require(url.startsWith("jdbc:h2:file:")||url.startsWith("jdbc:h2:mem:"),"Local demonstration dataset requires H2");
  }
  var sql=ctx.getBean(JdbcTemplate.class);var mapper=ctx.getBean(ObjectMapper.class);var validator=ctx.getBean(Validator.class);
   var tx=new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
   tx.executeWithoutResult(t->{try{
    if(sql.queryForObject("select count(*) from training_demo_seeds where id=?",Integer.class,MARK)>0){LOG.info("Training dataset already initialized; existing records preserved");return;}
    for(String table:List.of("training_exercises","training_programs","training_workouts","training_schedules")) {
     if(sql.queryForObject("select count(*) from "+table,Integer.class)>0) {
      LOG.warn("Training data already exists without the dataset marker; automatic population skipped");return;
     }
    }
    var exerciseService=ctx.getBean(TrainingExerciseService.class);var programs=ctx.getBean(TrainingProgramService.class);var workouts=ctx.getBean(TrainingWorkoutService.class);var schedules=ctx.getBean(TrainingScheduleService.class);
    var definitions=mapper.readValue(new ClassPathResource("training/demo/exercises.json").getInputStream(),ExerciseInput[].class);
    require(definitions.length==80,"Expected 80 exercises");
    List<Long> exerciseIds=new ArrayList<>();
    for(var e:definitions){valid(validator,e);exerciseIds.add(exerciseService.create(e).id());}
    var drafts=mapper.readValue(new ClassPathResource("training/demo/programmes.json").getInputStream(),ProgramInput[].class);
    require(drafts.length==12,"Expected 12 programmes");
    List<ProgramOutput> first=new ArrayList<>(),current=new ArrayList<>();
    var today=LocalDate.now(ctx.getBean(Clock.class).withZone(ZONE));List<Instant[]> programTimes=new ArrayList<>();
    for(int pidx=0;pidx<12;pidx++){
     var raw=drafts[pidx]; var draft=new ProgramInput(raw.name(),raw.description(),raw.objective(),raw.sessions().stream().map(s->new ProgramInput.SessionInput(s.name(),s.prescriptions().stream().map(e->new ProgramInput.PrescriptionInput(null,exerciseIds.get(e.exerciseId().intValue()-1),e.sets(),e.repetitions(),e.durationSeconds(),e.restSeconds(),e.targetLoadKg())).toList())).toList());valid(validator,draft);var initial=programs.create(draft);first.add(initial);
     List<ProgramInput.SessionInput> sessions=new ArrayList<>();
     for(var s:draft.sessions()){
      List<ProgramInput.PrescriptionInput> ps=new ArrayList<>();
      for(var e:s.prescriptions()){
       BigDecimal load=e.targetLoadKg();var equipment=definitions[exerciseIds.indexOf(e.exerciseId())].equipment();
       if(load!=null&&(equipment==Equipment.BARBELL||equipment==Equipment.DUMBBELLS))load=load.add(new BigDecimal(equipment==Equipment.BARBELL?"2.5":"1.0"));
       Integer reps=e.repetitions()==null?null:e.repetitions()+(pidx==1?0:1);
       Integer duration=e.durationSeconds()==null?null:e.durationSeconds()+(e.durationSeconds()>=600?60:5);
       ps.add(new ProgramInput.PrescriptionInput(null,e.exerciseId(),e.sets(),reps,duration,e.restSeconds(),load));
      }
      sessions.add(new ProgramInput.SessionInput(s.name(),ps));
     }
     var revised=new ProgramInput(draft.name(),draft.description()+" Révision 2 : progression mesurée des cibles, sans changement des exercices.",draft.objective(),sessions);valid(validator,revised);
     var updated=programs.update(initial.id(),new ProgramInput.Update(1,revised));current.add(updated);require(updated.currentRevision()==2,"Revision not created");
     Instant created=today.minusDays(91).atTime(8,0).atZone(ZONE).toInstant();
     Instant revisedAt=today.minusDays(89-(36+pidx)).atTime(6,0).atZone(ZONE).toInstant();programTimes.add(new Instant[]{created,revisedAt});
    }
    for(int i=0;i<90;i++){
     int pi=i%12, exposure=i/12;var programme=exposure<3?first.get(pi):current.get(pi);var session=programme.version().sessions().get(exposure%2);
     var begin=new WorkoutInput.Start(programme.id(),programme.version().revision(),session.id());valid(validator,begin);var w=workouts.start(begin);
     int duration=pi==7?18*60:pi==5?40*60:pi==6?30*60:pi==8?24*60:40*60+(i%5)*180;
     Instant started=today.minusDays(89-i).atTime(7+(i%2),10).atZone(ZONE).toInstant();
     if(started.plusSeconds(duration).isAfter(ctx.getBean(Clock.class).instant())) started=ctx.getBean(Clock.class).instant().minusSeconds(duration+60L);
     for(var ex:w.exercises())for(var set:ex.sets()){
      boolean skip=(i%17==0&&set.position()==ex.plannedSets()&&ex.position()==w.exercises().size());
      if(skip){workouts.skipSet(w.id(),set.id());continue;}
      int shortfall=exposure==0?2:exposure==1?1:0;
      Integer reps=ex.targetRepetitions()==null?null:Math.max(1,ex.targetRepetitions()-shortfall+((exposure>=5&&set.position()==1)?1:0));
      Integer seconds=ex.targetDurationSeconds()==null?null:Math.max(10,ex.targetDurationSeconds()-shortfall*5+((exposure>=5)?5:0));
      BigDecimal load=ex.targetLoadKg();
      if(load!=null&&(ex.equipment()==Equipment.BARBELL||ex.equipment()==Equipment.DUMBBELLS)){
       double factor=exposure==0?.9:exposure==1?.95:1;
       double increment=ex.equipment()==Equipment.BARBELL?2.5:1;
       load=BigDecimal.valueOf(Math.max(increment,Math.floor(load.doubleValue()*factor/increment)*increment));
      }
      var performance=new WorkoutInput.SetPerformance(reps,seconds,load);valid(validator,performance);workouts.saveSet(w.id(),set.id(),performance);
     }
     workouts.complete(w.id(),new WorkoutInput.Complete(false,"Démonstration synthétique : historique fictif, charges et durée simulées."));
     sql.update("update training_workouts set started_at=?,ended_at=? where id=?",java.sql.Timestamp.from(started),java.sql.Timestamp.from(started.plusSeconds(duration)),w.id());
    }
    // Correct only our new fixture timestamps; immutable historical snapshots are otherwise untouched.
    for(int i=0;i<12;i++){
     var p=current.get(i);var times=programTimes.get(i);
     sql.update("update training_programs set created_at=?,updated_at=? where id=?",java.sql.Timestamp.from(times[0]),java.sql.Timestamp.from(times[1]),p.id());
     sql.update("update training_program_versions set created_at=? where program_id=? and revision=1",java.sql.Timestamp.from(times[0]),p.id());
     sql.update("update training_program_versions set created_at=? where program_id=? and revision=2",java.sql.Timestamp.from(times[1]),p.id());
    }
    sql.update("update training_exercises set created_at=?,updated_at=?",java.sql.Timestamp.from(today.minusDays(92).atStartOfDay(ZONE).toInstant()),java.sql.Timestamp.from(today.minusDays(92).atStartOfDay(ZONE).toInstant()));
    for(int i=0;i<20;i++){
     var p=current.get(i%12);var s=p.version().sessions().get(i%2);
     var date=today.plusDays(1+i*2).atTime(i%2==0?18:7,30);
     var input=new ScheduleDto.Create(p.id(),2,s.id(),date);valid(validator,input);schedules.create(input);
    }
    sql.update("insert into training_demo_seeds(id) values(?)",MARK);
    require(sql.queryForObject("select count(*) from training_workouts where status='COMPLETED'",Integer.class)==90,"Workout count mismatch");
    LOG.info("Training dataset initialized: 80 exercises, 12 programmes, 24 revisions, 90 workouts, 1211 sets, 20 schedules");
   }catch(Exception e){throw new IllegalStateException("Integration fixture transaction rolled back",e);}});
 }
}
