package com.sportvision.training;
import com.sportvision.SportVisionApplication;
import com.sportvision.training.bootstrap.TrainingLocalDatasetInitializer;
import com.sportvision.training.dto.ExerciseInput;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes=SportVisionApplication.class,properties={
 "spring.datasource.url=jdbc:h2:mem:local_dataset_startup;DB_CLOSE_DELAY=-1",
 "spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false","sportvision.demo.enabled=false",
 "training.ai.api-key=","training.dataset.enabled=true"})
@ActiveProfiles({"test","local-demo"})
class TrainingLocalDatasetStartupTest {
 @Autowired JdbcTemplate sql;
 @Autowired TrainingLocalDatasetInitializer initializer;
 @Autowired TrainingExerciseService exercises;
 @Autowired TrainingAdaptationService adaptation;
 @MockBean GeminiClient gemini;
 int count(String table){return sql.queryForObject("select count(*) from "+table,Integer.class);}
 @Test void createsCompleteVersionedHistoryWithoutCallingGemini(){
  assertThat(count("training_exercises")).isEqualTo(80);
  assertThat(count("training_programs")).isEqualTo(12);
  assertThat(count("training_program_versions")).isEqualTo(24);
  assertThat(count("training_workouts")).isEqualTo(90);
  assertThat(count("training_workout_sets")).isEqualTo(1211);
  assertThat(count("training_schedules")).isEqualTo(20);
  assertThat(sql.queryForObject("select count(*) from training_workouts where ended_at<=started_at or status<>'COMPLETED'",Integer.class)).isZero();
  assertThat(sql.queryForObject("select count(*) from training_workouts w join training_program_versions v on v.id=w.program_version_id where v.program_id<>w.program_id or v.revision<>w.program_revision or w.started_at<v.created_at",Integer.class)).isZero();
  for(long id=1;id<=12;id++)assertThat(adaptation.report(id,2).eligible()).isTrue();
  verifyNoInteractions(gemini);
 }
 @Test @Transactional void repeatStartupPreservesEditsAndNewRecords()throws Exception{
  sql.update("update training_exercises set name='Custom user exercise' where id=1");
  exercises.create(new ExerciseInput("New user exercise","Description","Instructions",MuscleGroup.CORE,Equipment.NONE,Difficulty.BEGINNER,MeasurementType.REPETITIONS));
  initializer.run(new DefaultApplicationArguments());
  assertThat(count("training_exercises")).isEqualTo(81);
  assertThat(sql.queryForObject("select name from training_exercises where id=1",String.class)).isEqualTo("Custom user exercise");
  assertThat(count("training_workouts")).isEqualTo(90);
  assertThat(count("training_workout_sets")).isEqualTo(1211);
  verifyNoInteractions(gemini);
 }
 @Test @Transactional void unmarkedNonemptyDatabaseIsNeverPopulatedOrOverwritten()throws Exception{
  sql.update("delete from training_demo_seeds");
  sql.update("update training_exercises set name='Preserve existing data' where id=1");
  initializer.run(new DefaultApplicationArguments());
  assertThat(count("training_exercises")).isEqualTo(80);
  assertThat(count("training_demo_seeds")).isZero();
  assertThat(sql.queryForObject("select name from training_exercises where id=1",String.class)).isEqualTo("Preserve existing data");
 }
}
