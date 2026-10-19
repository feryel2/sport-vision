package com.sportvision.training;

import com.fasterxml.jackson.databind.*;
import com.sportvision.entity.*;
import com.sportvision.repository.UserRepository;
import com.sportvision.security.JwtService;
import com.sportvision.training.bootstrap.*;
import com.sportvision.training.service.GeminiClient;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=com.sportvision.SportVisionApplication.class,properties={
 "spring.datasource.url=jdbc:h2:mem:training_personal_demo;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000",
 "spring.jpa.hibernate.ddl-auto=create-drop","training.dataset.enabled=true",
 "training.demo.enabled=false","sportvision.demo.enabled=false","training.ai.api-key="})
@ActiveProfiles({"test","local-demo"}) @AutoConfigureMockMvc
class TrainingPersonalDemoTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate sql;
 @Autowired UserRepository users; @Autowired JwtService jwt;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Autowired TrainingPersonalDemoInitializer personal;
 @Autowired com.sportvision.training.service.TrainingProgramService programmeService;
 @MockBean GeminiClient gemini;
 record Account(String owner,String authorization) {}
 Account account() {
  var u=users.saveAndFlush(new User("demo-"+UUID.randomUUID()+"@example.invalid","not-a-login-password","Synthetic user",null,Role.ROLE_USER));
  return new Account("training-user:"+u.getId(),"Bearer "+jwt.generateToken(u.getEmail(),u.getRole().name()));
 }
 JsonNode data(org.springframework.test.web.servlet.ResultActions action)throws Exception {
  return mapper.readTree(action.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
 }
 JsonNode programmes(Account a)throws Exception {
  return data(mvc.perform(get("/api/training/programs?size=50").header("Authorization",a.authorization())).andExpect(status().isOk()));
 }
 int count(String table,String owner) {return sql.queryForObject("select count(*) from "+table+" where owner_reference=?",Integer.class,owner);}
 long programme(Account a,String name) {return sql.queryForObject("select id from training_programs where owner_reference=? and name=?",Long.class,a.owner(),name);}
 JsonNode report(Account a,long id)throws Exception {
  return data(mvc.perform(get("/api/training/ai/programs/"+id+"/adaptation?expectedRevision=2").header("Authorization",a.authorization())).andExpect(status().isOk()));
 }
 void counts(Account a) {
  assertThat(count("training_programs",a.owner())).isEqualTo(12);
  assertThat(count("training_workouts",a.owner())).isEqualTo(90);
  assertThat(count("training_schedules",a.owner())).isEqualTo(20);
  assertThat(sql.queryForObject("select count(*) from training_program_versions v join training_programs p on p.id=v.program_id where p.owner_reference=?",Integer.class,a.owner())).isEqualTo(24);
  assertThat(sql.queryForObject("select count(*) from training_workout_sets s join training_workout_exercises e on e.id=s.exercise_id join training_workouts w on w.id=e.workout_id where w.owner_reference=?",Integer.class,a.owner())).isEqualTo(1211);
 }
 @Test void firstAuthenticatedAccessCreatesPrivateCompleteHistoryAndProgress()throws Exception {
  Account a=account();assertThat(count("training_programs",a.owner())).isZero();
  assertThat(programmes(a).get("totalElements").asInt()).isEqualTo(12);counts(a);
  assertThat(sql.queryForObject("select count(*) from training_exercises",Integer.class)).isEqualTo(80);
  var details=data(mvc.perform(get("/api/training/programs/"+programme(a,"Full Body Débutant")).header("Authorization",a.authorization())).andExpect(status().isOk()));
  assertThat(details.at("/version/sessions").size()).isEqualTo(2);
  assertThat(details.at("/version/sessions/0/prescriptions").size()).isPositive();
  var history=data(mvc.perform(get("/api/training/workouts?status=COMPLETED&size=50").header("Authorization",a.authorization())).andExpect(status().isOk()));
  assertThat(history.get("totalElements").asInt()).isEqualTo(90);
  long workout=history.at("/items/0/id").asLong();
  var workoutDetails=data(mvc.perform(get("/api/training/workouts/"+workout).header("Authorization",a.authorization())).andExpect(status().isOk()));
  assertThat(workoutDetails.get("exercises").size()).isPositive();
  assertThat(sql.queryForObject("select count(*) from training_workouts w join training_program_versions v on v.id=w.program_version_id join training_programs p on p.id=v.program_id where w.owner_reference=? and (w.program_id<>p.id or w.program_revision<>v.revision or w.owner_reference<>p.owner_reference or w.started_at<v.created_at or w.ended_at<=w.started_at)",Integer.class,a.owner())).isZero();
  assertThat(sql.queryForObject("select count(*) from training_workout_exercises e join training_workouts w on w.id=e.workout_id join training_prescriptions p on p.id=e.source_prescription_id join training_program_sessions s on s.id=p.session_id where w.owner_reference=? and s.version_id<>w.program_version_id",Integer.class,a.owner())).isZero();
  for (int days:List.of(7,30,90)) {
   var stats=data(mvc.perform(get("/api/training/progress?days="+days).header("Authorization",a.authorization())).andExpect(status().isOk()));
   assertThat(stats.at("/indicators/completedWorkouts").asLong()).isPositive();
   assertThat(stats.at("/indicators/doneSets").asLong()).isPositive();
   String from=java.time.LocalDate.now(java.time.ZoneId.of("Africa/Tunis")).minusDays(days-1).toString();
   assertThat(sql.queryForObject("select count(*) from training_workouts where owner_reference=? and ended_at>=?",Integer.class,a.owner(),java.time.LocalDate.parse(from).atStartOfDay(java.time.ZoneId.of("Africa/Tunis")).toOffsetDateTime())).isPositive();
  }
  verifyNoInteractions(gemini);
 }
 @Test void repeatedRequestsAndRecreatedInitializerDoNotDuplicateOrOverwriteEdits()throws Exception {
  Account a=account();programmes(a);long id=programme(a,"Full Body Débutant");
  sql.update("update training_programs set name='Personal edit to preserve' where id=?",id);
  programmes(a);personal.prepare(a.owner());
  new TrainingPersonalDemoInitializer(sql,transactions,java.time.Clock.systemUTC()).prepare(a.owner());
  counts(a);assertThat(sql.queryForObject("select name from training_programs where id=?",String.class,id)).isEqualTo("Personal edit to preserve");
 }
 @Test void concurrentFirstAccessCreatesOnlyOneDataset()throws Exception {
  Account a=account();var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try {
   Callable<Integer> request=()->{gate.await();return programmes(a).get("totalElements").asInt();};
   var x=pool.submit(request);var y=pool.submit(request);gate.countDown();
   assertThat(x.get(60,TimeUnit.SECONDS)).isEqualTo(12);assertThat(y.get(60,TimeUnit.SECONDS)).isEqualTo(12);
  } finally {pool.shutdownNow();}
  counts(a);
 }
 @Test void existingPersonalProgrammeAndWorkoutAreNotOverwritten()throws Exception {
  Account a=account();String input="""
   {"name":"My existing programme","description":"Keep this","objective":"Keep goal","sessions":[{"name":"My session","prescriptions":[{"exerciseId":1,"sets":1,"repetitions":8,"restSeconds":30}]}]}
   """;
  var p=data(mvc.perform(post("/api/training/programs").header("Authorization",a.authorization()).contentType("application/json").content(input)).andExpect(status().isCreated()));
  long id=p.get("id").asLong();long session=p.at("/version/sessions/0/id").asLong();
  var w=data(mvc.perform(post("/api/training/workouts").header("Authorization",a.authorization()).contentType("application/json").content("{\"programId\":"+id+",\"revision\":1,\"sessionId\":"+session+"}")).andExpect(status().isOk()));
  String scheduled=java.time.LocalDate.now(java.time.ZoneId.of("Africa/Tunis")).plusDays(1)+"T18:30:00";
  var existingSchedule=data(mvc.perform(post("/api/training/schedules").header("Authorization",a.authorization()).contentType("application/json")
   .content("{\"programId\":"+id+",\"expectedRevision\":1,\"sessionId\":"+session+",\"scheduledLocal\":\""+scheduled+"\"}")).andExpect(status().isCreated()));
  long scheduleId=existingSchedule.get("id").asLong();
  var scheduleBefore=sql.queryForMap("select * from training_schedules where id=?",scheduleId);
  programmes(a);
  assertThat(sql.queryForMap("select * from training_schedules where id=?",scheduleId)).isEqualTo(scheduleBefore);
  assertThat(count("training_schedules",a.owner())).isEqualTo(21);
  assertThat(count("training_programs",a.owner())).isEqualTo(13);assertThat(count("training_workouts",a.owner())).isEqualTo(91);
  mvc.perform(get("/api/training/programs/"+id).header("Authorization",a.authorization())).andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("My existing programme"));
  mvc.perform(get("/api/training/workouts/"+w.get("id").asLong()).header("Authorization",a.authorization())).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
 }
 @Test void copiedUsersRemainIsolatedAndSourceDatasetUnchanged()throws Exception {
  Account a=account(),b=account();programmes(a);programmes(b);counts(a);counts(b);
  long id=programme(a,"Full Body Débutant");
  mvc.perform(get("/api/training/programs/"+id).header("Authorization",b.authorization())).andExpect(status().isNotFound());
  long workout=sql.queryForObject("select min(id) from training_workouts where owner_reference=?",Long.class,a.owner());
  mvc.perform(get("/api/training/workouts/"+workout).header("Authorization",b.authorization())).andExpect(status().isNotFound());
  assertThat(count("training_programs","training-local-demo")).isEqualTo(12);assertThat(count("training_workouts","training-local-demo")).isEqualTo(90);
  mvc.perform(get("/api/training/programs")).andExpect(status().isUnauthorized());
 }
 @Test void failedCopyRollsBackAllPersonalRecordsAndCanBeRetried()throws Exception {
  Account a=account();
  long source=sql.queryForObject("select min(e.id) from training_workout_exercises e join training_workouts w on w.id=e.workout_id where w.owner_reference='training-local-demo'",Long.class);
  long prescription=sql.queryForObject("select source_prescription_id from training_workout_exercises where id=?",Long.class,source);
  try {
   sql.update("update training_workout_exercises set source_prescription_id=999999999 where id=?",source);
   org.assertj.core.api.Assertions.assertThatThrownBy(()->personal.prepare(a.owner())).isInstanceOf(IllegalStateException.class);
   assertThat(count("training_programs",a.owner())).isZero();assertThat(count("training_workouts",a.owner())).isZero();
   assertThat(sql.queryForObject("select count(*) from training_demo_seeds where id=?",Integer.class,"personal-demo-v1:"+a.owner())).isZero();
  } finally {sql.update("update training_workout_exercises set source_prescription_id=? where id=?",prescription,source);}
  personal.prepare(a.owner());counts(a);
 }
 @Test void laterFirstAccessRebasesOnlyNewCopiesToKeepRecentChartsMeaningful() {
  Account a=account();
  var latest=sql.queryForObject("select max(ended_at) from training_workouts where owner_reference='training-local-demo'",java.time.OffsetDateTime.class);
  var future=java.time.Clock.offset(java.time.Clock.systemUTC(),java.time.Duration.ofDays(40));
  new TrainingPersonalDemoInitializer(sql,transactions,future).prepare(a.owner());counts(a);
  var copied=sql.queryForObject("select max(ended_at) from training_workouts where owner_reference=?",java.time.OffsetDateTime.class,a.owner());
  assertThat(copied.atZoneSameInstant(java.time.ZoneId.of("Africa/Tunis")).toLocalDate()).isEqualTo(java.time.LocalDate.now(future.withZone(java.time.ZoneId.of("Africa/Tunis"))));
  assertThat(sql.queryForObject("select max(ended_at) from training_workouts where owner_reference='training-local-demo'",java.time.OffsetDateTime.class)).isEqualTo(latest);
 }
 @Test void unrelatedLegacySharedOwnerProgrammeIsNotBroadcastToNewUsers()throws Exception {
  Account a=account();
  org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
  var legacy=programmeService.create(new com.sportvision.training.dto.ProgramInput("Unrelated legacy programme","Must remain untouched","Private legacy goal",List.of(
   new com.sportvision.training.dto.ProgramInput.SessionInput("Legacy session",List.of(
    new com.sportvision.training.dto.ProgramInput.PrescriptionInput(null,1L,2,10,null,60,null))))));
  long id=legacy.id();
  try {
   programmes(a);counts(a);
   assertThat(sql.queryForObject("select count(*) from training_programs where owner_reference=? and name='Unrelated legacy programme'",Integer.class,a.owner())).isZero();
   assertThat(sql.queryForObject("select name from training_programs where id=?",String.class,id)).isEqualTo("Unrelated legacy programme");
  } finally {
   sql.update("delete from training_prescriptions where session_id in (select s.id from training_program_sessions s join training_program_versions v on v.id=s.version_id where v.program_id=?)",id);
   sql.update("delete from training_program_sessions where version_id in (select id from training_program_versions where program_id=?)",id);
   sql.update("delete from training_program_versions where program_id=?",id);sql.update("delete from training_programs where id=?",id);
  }
 }
 @Test void twoScenariosAreEligibleAndMockedAdaptationPreservesHistory()throws Exception {
  Account a=account();programmes(a);
  long improve=programme(a,"Full Body Débutant"),difficult=programme(a,"Force fondamentale");
  var good=report(a,improve);var low=report(a,difficult);
  assertThat(good.get("eligible").asBoolean()).isTrue();assertThat(low.get("eligible").asBoolean()).isTrue();
  assertThat(good.toString()).contains("INCREASE_TARGET");assertThat(low.toString()).contains("REDUCE_TARGET");
  assertThat(low.toString()).doesNotContain("INCREASE_TARGET");
  for (long id:List.of(improve,difficult)) {
   var r=report(a,id);JsonNode chosen=null;
   for (var item:r.get("items")) if (!item.get("options").isEmpty()) {chosen=item;break;}
   assertThat(chosen).isNotNull();String option=chosen.at("/options/0/code").asText();int slot=chosen.get("slot").asInt();
   when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"adjustments\":[{\"slot\":"+slot+",\"option\":\""+option+"\"}]}");
   String base="/api/training/ai/programs/"+id+"/adaptation";
   var preview=data(mvc.perform(post(base).header("Authorization",a.authorization()).contentType("application/json").content("{\"expectedRevision\":2}")).andExpect(status().isOk()));
   assertThat(preview.at("/changes/0/before")).isNotEqualTo(preview.at("/changes/0/after"));
   assertThat(preview.at("/changes/0/before")).isEqualTo(chosen.get("before"));
   var histories=sql.queryForList("select id,program_version_id,program_revision,started_at,ended_at,total_sets,done_sets,skipped_sets from training_workouts where owner_reference=? order by id",a.owner());
   var sets=sql.queryForList("select s.* from training_workout_sets s join training_workout_exercises e on e.id=s.exercise_id join training_workouts w on w.id=e.workout_id where w.owner_reference=? order by s.id",a.owner());
   mvc.perform(post(base+"/confirm").header("Authorization",a.authorization()).contentType("application/json").content("{\"expectedRevision\":2,\"proposalId\":\""+preview.get("proposalId").asText()+"\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.currentRevision").value(3));
   mvc.perform(get("/api/training/programs/"+id+"/revisions/2").header("Authorization",a.authorization())).andExpect(status().isOk());
   assertThat(sql.queryForList("select id,program_version_id,program_revision,started_at,ended_at,total_sets,done_sets,skipped_sets from training_workouts where owner_reference=? order by id",a.owner())).isEqualTo(histories);
   assertThat(sql.queryForList("select s.* from training_workout_sets s join training_workout_exercises e on e.id=s.exercise_id join training_workouts w on w.id=e.workout_id where w.owner_reference=? order by s.id",a.owner())).isEqualTo(sets);
  }
 }
}
