package com.sportvision.training;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.sportvision.SportVisionApplication.class, properties={"spring.datasource.url=jdbc:h2:mem:training_workout_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(username="training-test-user", roles="ADMIN")
class TrainingWorkoutApiTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;
    @MockBean TrainingCurrentUserProvider user;
    long exerciseId;
    @BeforeEach void setup() throws Exception {
        for(String table:new String[]{"training_workout_sets","training_workout_exercises","training_workouts","training_prescriptions","training_program_sessions","training_program_versions","training_programs","training_exercises"})jdbc.update("delete from "+table);
        when(user.ownerReference()).thenReturn("alice");
        exerciseId=data(mvc.perform(post("/api/training/exercises").contentType("application/json").content("""
            {"name":"Squat","description":"Description","instructions":"Consignes","muscleGroup":"LEGS","equipment":"NONE","difficulty":"BEGINNER","measurementType":"REPETITIONS"}
            """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("id").asLong();
    }
    JsonNode data(String json)throws Exception{return mapper.readTree(json).get("data");}
    ObjectNode programInput()throws Exception{return (ObjectNode)mapper.readTree("""
        {"name":"Programme initial","description":"Description initiale","objective":"Objectif","sessions":[{"name":"Séance initiale","prescriptions":[{"exerciseId":%d,"sets":3,"repetitions":10,"restSeconds":30,"targetLoadKg":5}]}]}
        """.formatted(exerciseId));}
    JsonNode program()throws Exception{return data(mvc.perform(post("/api/training/programs").contentType("application/json").content(programInput().toString())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
    String startInput(JsonNode p){return "{\"programId\":"+p.get("id")+",\"revision\":"+p.get("currentRevision")+",\"sessionId\":"+p.at("/version/sessions/0/id")+"}";}
    JsonNode start(JsonNode p)throws Exception{return data(mvc.perform(post("/api/training/workouts").contentType("application/json").content(startInput(p))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
    JsonNode workout(long id)throws Exception{return data(mvc.perform(get("/api/training/workouts/"+id)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}

    @Test void changingProgramAfterStartDoesNotChangeSnapshot()throws Exception {
        JsonNode p=program();JsonNode started=start(p);long id=started.get("id").asLong();
        assertThat(started.get("doneSets").asInt()).isZero();assertThat(started.get("pendingSets").asInt()).isEqualTo(3);
        assertThat(started.at("/exercises/0/sets/0/repetitions").isNull()).isTrue();
        assertThat(start(p).get("id")).isEqualTo(started.get("id"));
        ObjectNode input=programInput();input.put("name","Programme modifié");
        ((ObjectNode)input.at("/sessions/0")).put("name","Autre séance");
        ((ObjectNode)input.at("/sessions/0/prescriptions/0")).put("repetitions",20).put("existingPrescriptionId",p.at("/version/sessions/0/prescriptions/0/id").asLong());
        ObjectNode update=mapper.createObjectNode().put("expectedRevision",1);update.set("program",input);
        mvc.perform(put("/api/training/programs/"+p.get("id").asLong()).contentType("application/json").content(update.toString())).andExpect(status().isOk());
        mvc.perform(put("/api/training/exercises/"+exerciseId).contentType("application/json").content("""
            {"name":"Nouveau catalogue","description":"Description changée","instructions":"Autres consignes","muscleGroup":"LEGS","equipment":"DUMBBELLS","difficulty":"BEGINNER","measurementType":"DURATION"}
            """)).andExpect(status().isOk());
        JsonNode after=workout(id);assertThat(after.get("programName").asText()).isEqualTo("Programme initial");
        assertThat(after.get("sessionName").asText()).isEqualTo("Séance initiale");
        assertThat(after.get("programRevision").asInt()).isEqualTo(1);
        assertThat(after.get("exercises")).isEqualTo(started.get("exercises"));
    }
    @Test void anotherOwnerCannotReadOrModifyWorkoutOrItsSets()throws Exception {
        JsonNode sourceProgram=program();JsonNode alice=start(sourceProgram);long id=alice.get("id").asLong(),set=alice.at("/exercises/0/sets/0/id").asLong();
        when(user.ownerReference()).thenReturn("bob");
        mvc.perform(get("/api/training/workouts/"+id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/training/workouts/current")).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get("/api/training/workouts")).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(post("/api/training/workouts").contentType("application/json").content(startInput(sourceProgram))).andExpect(status().isNotFound());
        mvc.perform(put("/api/training/workouts/"+id+"/sets/"+set).contentType("application/json").content("{\"repetitions\":8}")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/training/workouts/"+id+"/sets/"+set+"/skip")).andExpect(status().isNotFound());
        for(String action:new String[]{"complete","abandon"})mvc.perform(post("/api/training/workouts/"+id+"/"+action).contentType("application/json").content("{}")).andExpect(status().isNotFound());
        long bob=start(program()).get("id").asLong();
        mvc.perform(put("/api/training/workouts/"+bob+"/sets/"+set).contentType("application/json").content("{\"repetitions\":8}")).andExpect(status().isNotFound());
        when(user.ownerReference()).thenReturn("alice");assertThat(workout(id).get("doneSets").asInt()).isZero();
    }
    @Test void repeatedFinalizationIsIdempotentAndClosedWorkoutsRejectChanges()throws Exception {
        JsonNode p=program();JsonNode w=start(p);long id=w.get("id").asLong(),set=w.at("/exercises/0/sets/0/id").asLong();
        mvc.perform(post("/api/training/workouts/"+id+"/complete").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/training/workouts/"+id+"/sets/"+set).contentType("application/json").content("{\"repetitions\":8,\"durationSeconds\":30}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/training/workouts/"+id+"/sets/"+set).contentType("application/json").content("{\"repetitions\":8,\"loadKg\":4}")).andExpect(status().isOk());
        mvc.perform(post("/api/training/workouts/"+id+"/complete").contentType("application/json").content("{}")).andExpect(status().isConflict()).andExpect(jsonPath("$.data.code").value("PENDING_CONFIRMATION_REQUIRED")).andExpect(jsonPath("$.data.pendingSets").value(2));
        String finish="{\"confirmSkipPending\":true,\"comment\":\"Séance terminée\"}";
        JsonNode first=data(mvc.perform(post("/api/training/workouts/"+id+"/complete").contentType("application/json").content(finish)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode again=data(mvc.perform(post("/api/training/workouts/"+id+"/complete").contentType("application/json").content(finish)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(again).isEqualTo(first);assertThat(first.get("doneSets").asInt()).isEqualTo(1);assertThat(first.get("skippedSets").asInt()).isEqualTo(2);
        assertThat(first.at("/exercises/0/sets/1/repetitions").isNull()).isTrue();assertThat(jdbc.queryForObject("select count(*) from training_workout_sets",Long.class)).isEqualTo(3);
        mvc.perform(put("/api/training/workouts/"+id+"/sets/"+set).contentType("application/json").content("{\"repetitions\":9}")).andExpect(status().isConflict());
        mvc.perform(patch("/api/training/workouts/"+id+"/sets/"+set+"/skip")).andExpect(status().isConflict());
        mvc.perform(get("/api/training/workouts/current")).andExpect(jsonPath("$.data").doesNotExist());
        JsonNode next=start(p);long other=next.get("id").asLong(),otherSet=next.at("/exercises/0/sets/0/id").asLong();
        mvc.perform(put("/api/training/workouts/"+other+"/sets/"+otherSet).contentType("application/json").content("{\"repetitions\":6}")).andExpect(status().isOk());
        mvc.perform(post("/api/training/workouts/"+other+"/abandon").contentType("application/json").content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.doneSets").value(1)).andExpect(jsonPath("$.data.status").value("ABANDONED"));
        mvc.perform(post("/api/training/workouts/"+other+"/complete").contentType("application/json").content(finish)).andExpect(status().isConflict());
        mvc.perform(put("/api/training/workouts/"+other+"/sets/"+otherSet).contentType("application/json").content("{\"repetitions\":9}")).andExpect(status().isConflict());
    }
}
