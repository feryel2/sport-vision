package com.sportvision.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvision.entity.Role;
import com.sportvision.entity.User;
import com.sportvision.repository.UserRepository;
import com.sportvision.security.JwtService;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.service.GeminiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=com.sportvision.SportVisionApplication.class, properties={
 "spring.datasource.url=jdbc:h2:mem:training_jwt_tests;DB_CLOSE_DELAY=-1",
 "spring.jpa.hibernate.ddl-auto=create-drop", "training.dataset.enabled=true",
 "training.demo.enabled=false", "sportvision.demo.enabled=false", "training.ai.api-key="})
@ActiveProfiles({"test","local-demo"}) @AutoConfigureMockMvc
class TrainingJwtIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired UserRepository users;
 @Autowired JwtService jwt; @Autowired JdbcTemplate sql; @Autowired TrainingCurrentUserProvider owner;
 @MockBean GeminiClient gemini;
 String token(String name) {
  String email=name+"@example.invalid";
  var u=users.findByEmail(email).orElseGet(()->users.saveAndFlush(new User(email,"not-a-login-password",name,null,Role.ROLE_USER)));
  return "Bearer "+jwt.generateToken(u.getEmail(),u.getRole().name());
 }
 JsonNode create(String auth) throws Exception {
  String input="""
   {"name":"Private programme","description":"Personal test","objective":"Strength",
    "sessions":[{"name":"Session","prescriptions":[{"exerciseId":1,"sets":2,"repetitions":10,"restSeconds":60}]}]}
   """;
  return mapper.readTree(mvc.perform(post("/api/training/programs").header("Authorization",auth)
   .contentType("application/json").content(input)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("data");
 }
 @Test void missingAndInvalidJwtAreRejectedBeforeTraining() throws Exception {
  mvc.perform(get("/api/training/programs")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/training/ai/options").header("Authorization","Bearer invalid-token")).andExpect(status().isUnauthorized());
  verifyNoInteractions(gemini);
 }
 @Test void authenticatedCatalogueAndAiOptionsWorkWithActualJwt() throws Exception {
  String auth=token("catalogue");
  mvc.perform(get("/api/training/exercises").header("Authorization",auth))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(80));
  mvc.perform(get("/api/training/ai/options").header("Authorization",auth)).andExpect(status().isOk());
  verifyNoInteractions(gemini);
 }
 @Test void jwtOwnersAreStablePrivateAndCannotAdoptDemoRecords() throws Exception {
  String a=token("owner-a"), b=token("owner-b");var p=create(a);long id=p.get("id").asLong();
  long uid=users.findByEmail("owner-a@example.invalid").orElseThrow().getId();
  assertThat(sql.queryForObject("select owner_reference from training_programs where id=?",String.class,id)).isEqualTo("training-user:"+uid);
  mvc.perform(get("/api/training/programs/"+id).header("Authorization",a)).andExpect(status().isOk());
  mvc.perform(get("/api/training/programs/"+id).header("Authorization",b)).andExpect(status().isNotFound());
  mvc.perform(post("/api/training/programs/"+id+"/duplicate").header("Authorization",b)).andExpect(status().isNotFound());
  Long demo=sql.queryForObject("select min(id) from training_programs where owner_reference='training-local-demo'",Long.class);
  mvc.perform(get("/api/training/programs/"+demo).header("Authorization",a)).andExpect(status().isNotFound());
  assertThat(sql.queryForObject("select count(*) from training_programs where owner_reference='training-local-demo'",Integer.class)).isEqualTo(12);
 }
 @Test void workoutAndScheduleOwnershipUseJwtWithoutChangingSnapshots() throws Exception {
  String a=token("workout-a"), b=token("workout-b");var p=create(a);long id=p.get("id").asLong();
  long session=p.at("/version/sessions/0/id").asLong();
  String body="{\"programId\":"+id+",\"revision\":1,\"sessionId\":"+session+"}";
  var w=mapper.readTree(mvc.perform(post("/api/training/workouts").header("Authorization",a).contentType("application/json").content(body))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data");
  long wid=w.get("id").asLong();
  mvc.perform(get("/api/training/workouts/"+wid).header("Authorization",b)).andExpect(status().isNotFound());
  mvc.perform(get("/api/training/progress?days=30").header("Authorization",a)).andExpect(status().isOk());
  String date=java.time.LocalDate.now(java.time.ZoneId.of("Africa/Tunis")).plusDays(2)+"T18:30:00";
  String scheduled="{\"programId\":"+id+",\"expectedRevision\":1,\"sessionId\":"+session+",\"scheduledLocal\":\""+date+"\"}";
  var s=mapper.readTree(mvc.perform(post("/api/training/schedules").header("Authorization",a).contentType("application/json").content(scheduled))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("data");
  mvc.perform(post("/api/training/schedules/"+s.get("id").asLong()+"/cancel").header("Authorization",b).contentType("application/json").content("{\"expectedVersion\":0}"))
   .andExpect(status().isNotFound());
  assertThat(sql.queryForObject("select program_revision from training_workouts where id=?",Integer.class,wid)).isEqualTo(1);
 }
 @Test void inactiveJwtAccountCannotReachTraining() throws Exception {
  String auth=token("inactive");var u=users.findByEmail("inactive@example.invalid").orElseThrow();u.setActive(false);users.saveAndFlush(u);
  mvc.perform(get("/api/training/programs").header("Authorization",auth)).andExpect(status().isUnauthorized());
 }
 @Test void authenticatedAdministratorCanManageTheSharedCatalogue() throws Exception {
  var admin=users.saveAndFlush(new User("training-admin@example.invalid","not-a-login-password","Training admin",null,Role.ROLE_ADMIN));
  String auth="Bearer "+jwt.generateToken(admin.getEmail(),admin.getRole().name());
  String input="""
   {"name":"Training admin check","description":"Synthetic exercise","instructions":"Controlled movement",
    "muscleGroup":"CORE","equipment":"NONE","difficulty":"BEGINNER","measurementType":"DURATION"}
   """;
  var item=mapper.readTree(mvc.perform(post("/api/training/exercises").header("Authorization",auth).contentType("application/json").content(input))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("data");
  long id=item.get("id").asLong();
  mvc.perform(patch("/api/training/exercises/"+id+"/archive").header("Authorization",auth)).andExpect(status().isOk());
  mvc.perform(patch("/api/training/exercises/"+id+"/restore").header("Authorization",auth)).andExpect(status().isOk());
  // Remove only this synthetic unreferenced exercise so the shared fixture count stays stable.
  sql.update("delete from training_exercises where id=?",id);
 }
 @Test void startupStillResolvesTheDedicatedDemoIdentity() {
  org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
  assertThat(owner.ownerReference()).isEqualTo("training-local-demo");
  assertThat(sql.queryForObject("select count(*) from training_workouts where owner_reference='training-local-demo'",Integer.class)).isEqualTo(90);
  verifyNoInteractions(gemini);
 }
}
