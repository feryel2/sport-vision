package com.sportvision.training;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.service.GeminiClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:training_schedule_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc
class TrainingScheduleApiTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;
    @MockBean TrainingCurrentUserProvider user;@MockBean Clock clock;@MockBean GeminiClient gemini;
    JsonNode program;ObjectNode input;long programId,sessionId;
    @BeforeEach void setup()throws Exception{
        for(String table:List.of("training_schedules","training_workout_sets","training_workout_exercises","training_workouts","training_prescriptions","training_program_sessions","training_program_versions","training_programs","training_exercises"))jdbc.update("delete from "+table);
        when(user.ownerReference()).thenReturn("schedule-owner");when(clock.instant()).thenReturn(Instant.parse("2030-01-01T08:00:00Z"));when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        long exercise=data(mvc.perform(post("/exercises").contentType("application/json").content("""
            {"name":"Squat","description":"Description","instructions":"Consignes","muscleGroup":"LEGS","equipment":"NONE","difficulty":"BEGINNER","measurementType":"REPETITIONS"}
            """)).andExpect(status().isCreated())).path("id").asLong();
        input=(ObjectNode)mapper.readTree("""
            {"name":"Programme","description":"Description","objective":"Objectif","sessions":[{"name":"Séance","prescriptions":[{"exerciseId":%d,"sets":1,"repetitions":10,"restSeconds":30}]}]}
            """.formatted(exercise));
        program=data(mvc.perform(post("/programs").contentType("application/json").content(input.toString())).andExpect(status().isCreated()));
        programId=program.path("id").asLong();sessionId=program.at("/version/sessions/0/id").asLong();
    }
    @AfterEach void noAi(){verifyNoInteractions(gemini);}
    JsonNode data(ResultActions r)throws Exception{return mapper.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");}
    ResultActions create(String date)throws Exception{return mvc.perform(post("/schedules").contentType("application/json").content("{\"programId\":"+programId+",\"expectedRevision\":1,\"sessionId\":"+sessionId+",\"scheduledLocal\":\""+date+"\"}"));}
    JsonNode scheduled()throws Exception{return data(create("2030-01-02T00:30").andExpect(status().isCreated()));}
    ResultActions action(JsonNode s,String action)throws Exception{return mvc.perform(post("/schedules/"+s.path("id").asLong()+"/"+action).contentType("application/json").content("{\"expectedVersion\":"+s.path("version")+"}"));}
    ResultActions update(JsonNode s,String date)throws Exception{return mvc.perform(put("/schedules/"+s.path("id").asLong()).contentType("application/json").content("{\"expectedVersion\":"+s.path("version")+",\"scheduledLocal\":\""+date+"\"}"));}
    ResultActions list(String from,String to)throws Exception{return mvc.perform(get("/schedules").param("from",from).param("to",to));}
    @Test void tunisDateAndInclusiveRangeAreIndependentOfServerTimezone()throws Exception{
        var s=scheduled();assertThat(s.path("scheduledAt").asText()).isEqualTo("2030-01-01T23:30:00Z");assertThat(s.path("timeZone").asText()).isEqualTo("Africa/Tunis");
        list("2030-01-02","2030-01-02").andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        list("2030-01-01","2030-01-01").andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        assertThat(jdbc.queryForObject("select count(*) from training_workouts",Integer.class)).isZero();
    }
    @Test void rejectsInvalidDatesInputsAndMismatchedSessions()throws Exception{
        for(String date:List.of("2029-12-31T12:00","2030-01-01T09:00","2030-01-02T12:00:01","not-a-date"))create(date).andExpect(status().isBadRequest());
        sessionId=999999;create("2030-01-02T12:00").andExpect(status().isBadRequest());
        mvc.perform(post("/schedules").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        list("2030-04-01","2030-01-01").andExpect(status().isBadRequest());list("2030-01-01","2031-01-01").andExpect(status().isBadRequest());
        mvc.perform(get("/schedules")).andExpect(status().isBadRequest());
        mvc.perform(get("/schedules").param("from","2030-01-01").param("to","2030-01-02").param("size","100")).andExpect(status().isBadRequest());
    }
    @Test void dateUpdateChecksVersionAndCancellationReleasesSlot()throws Exception{
        var s=scheduled();create("2030-01-02T00:30").andExpect(status().isConflict());
        var changed=data(update(s,"2030-01-03T10:00").andExpect(status().isOk()));assertThat(changed.path("version").asLong()).isGreaterThan(s.path("version").asLong());
        update(s,"2030-01-04T10:00").andExpect(status().isConflict());action(s,"cancel").andExpect(status().isConflict());
        var other=scheduled();update(other,"2030-01-03T10:00").andExpect(status().isConflict());
        action(changed,"cancel").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));action(changed,"cancel").andExpect(status().isOk());
        action(changed,"start").andExpect(status().isConflict());create("2030-01-03T10:00").andExpect(status().isCreated());
    }
    @Test void ownerIsAlwaysDeterminedByServer()throws Exception{
        var s=scheduled();when(user.ownerReference()).thenReturn("other-owner");
        list("2030-01-01","2030-01-31").andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        action(s,"start").andExpect(status().isNotFound());action(s,"cancel").andExpect(status().isNotFound());update(s,"2030-01-04T10:00").andExpect(status().isNotFound());create("2030-01-04T10:00").andExpect(status().isNotFound());
        when(user.ownerReference()).thenReturn(null);list("2030-01-01","2030-01-31").andExpect(status().isForbidden());
    }
    @Test void startIsRepeatableAndCompletedWorkoutStaysInExistingHistory()throws Exception{
        var s=scheduled();var w=data(action(s,"start").andExpect(status().isOk()));
        assertThat(data(action(s,"start").andExpect(status().isOk())).path("id")).isEqualTo(w.path("id"));
        action(s,"cancel").andExpect(status().isConflict());update(s,"2030-01-04T10:00").andExpect(status().isConflict());
        list("2030-01-01","2030-01-31").andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].status").value("IN_PROGRESS"));
        long wid=w.path("id").asLong(),set=w.at("/exercises/0/sets/0/id").asLong();
        mvc.perform(put("/workouts/"+wid+"/sets/"+set).contentType("application/json").content("{\"repetitions\":10}")).andExpect(status().isOk());
        mvc.perform(post("/workouts/"+wid+"/complete").contentType("application/json").content("{}")).andExpect(status().isOk());
        var snapshot=data(mvc.perform(get("/workouts/"+wid)).andExpect(status().isOk()));
        assertThat(data(action(s,"start").andExpect(status().isOk()))).isEqualTo(snapshot);
        list("2030-01-01","2030-01-31").andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].status").value("COMPLETED"));
        mvc.perform(get("/workouts").param("status","COMPLETED")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        assertThat(jdbc.queryForObject("select count(*) from training_workouts",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(1);
    }
    @Test void keepsPinnedRevisionWhenProgramChanges()throws Exception{
        var s=scheduled();input.put("name","Programme modifié");((ObjectNode)input.at("/sessions/0/prescriptions/0")).put("repetitions",20);
        ObjectNode update=mapper.createObjectNode().put("expectedRevision",1);update.set("program",input);
        mvc.perform(put("/programs/"+programId).contentType("application/json").content(update.toString())).andExpect(status().isOk());
        create("2030-01-04T10:00").andExpect(status().isConflict());
        action(s,"start").andExpect(status().isOk()).andExpect(jsonPath("$.data.programRevision").value(1)).andExpect(jsonPath("$.data.exercises[0].targetRepetitions").value(10));
    }
    @Test void archivedProgramsCannotBeScheduledOrStartedButCanBeCancelled()throws Exception{
        var s=scheduled();mvc.perform(patch("/programs/"+programId+"/archive").contentType("application/json").content("{\"expectedRevision\":1}")).andExpect(status().isOk());
        create("2030-01-04T10:00").andExpect(status().isConflict());action(s,"start").andExpect(status().isConflict());update(s,"2030-01-04T10:00").andExpect(status().isConflict());action(s,"cancel").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from training_workouts",Integer.class)).isZero();
    }
    @Test void reusesManualExecutionAndNeverLinksOneWorkoutToTwoSchedules()throws Exception{
        var s=scheduled();var other=data(create("2030-01-03T10:00").andExpect(status().isCreated()));
        var manual=data(mvc.perform(post("/workouts").contentType("application/json").content("{\"programId\":"+programId+",\"revision\":1,\"sessionId\":"+sessionId+"}")).andExpect(status().isOk()));
        assertThat(data(action(s,"start").andExpect(status().isOk())).path("id")).isEqualTo(manual.path("id"));
        action(other,"start").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from training_workouts",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from training_schedules where workout_id is not null",Integer.class)).isEqualTo(1);
    }
    @Test void abandonedExecutionIsPreservedWithoutCreatingAnotherWorkout()throws Exception{
        var s=scheduled();var w=data(action(s,"start").andExpect(status().isOk()));
        mvc.perform(post("/workouts/"+w.path("id").asLong()+"/abandon").contentType("application/json").content("{}")).andExpect(status().isOk());
        action(s,"start").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ABANDONED"));
        list("2030-01-01","2030-01-31").andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].status").value("ABANDONED"));
        assertThat(jdbc.queryForObject("select count(*) from training_workouts",Integer.class)).isEqualTo(1);
    }
    private List<Integer> concurrent(Callable<Integer> action)throws Exception{
        var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try{Callable<Integer> task=()->{gate.await();return action.call();};var a=pool.submit(task);var b=pool.submit(task);gate.countDown();return List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    @Test void concurrentCreationCannotDuplicateSlot()throws Exception{
        assertThat(concurrent(()->create("2030-01-02T10:00").andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(201,409);
        assertThat(jdbc.queryForObject("select count(*) from training_schedules",Integer.class)).isEqualTo(1);
    }
    @Test void concurrentStartCreatesExactlyOneWorkout()throws Exception{
        var s=scheduled();assertThat(concurrent(()->action(s,"start").andReturn().getResponse().getStatus())).containsOnly(200);
        assertThat(jdbc.queryForObject("select count(*) from training_workouts",Integer.class)).isEqualTo(1);
    }
}
