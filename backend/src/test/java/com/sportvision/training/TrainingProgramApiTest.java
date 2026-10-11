package com.sportvision.training;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:training_program_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc
class TrainingProgramApiTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc;
    @MockBean TrainingCurrentUserProvider user;
    @SpyBean TrainingProgramVersionRepository versionRepository;
    long repsId; long durationId;
    @BeforeEach void setup() throws Exception {
        for(String table:new String[]{"training_prescriptions","training_program_sessions","training_program_versions","training_programs","training_exercises"}) jdbc.update("delete from "+table);
        when(user.ownerReference()).thenReturn("alice");
        repsId=exercise("Squat","REPETITIONS");durationId=exercise("Gainage","DURATION");
    }
    long exercise(String name,String type) throws Exception {
        String json="""
            {"name":"%s","description":"Description","instructions":"Consignes","muscleGroup":"LEGS","equipment":"NONE","difficulty":"BEGINNER","measurementType":"%s"}
            """.formatted(name,type);
        return mapper.readTree(mvc.perform(post("/exercises").contentType("application/json").content(json)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).at("/data/id").asLong();
    }
    ObjectNode input() throws Exception {
        return (ObjectNode)mapper.readTree("""
          {"name":"Programme A","description":"Description A","objective":"Objectif A","ownerReference":"mallory",
           "sessions":[{"name":"Séance 1","prescriptions":[{"exerciseId":%d,"sets":3,"repetitions":12,"restSeconds":45,"targetLoadKg":5.5}]},
                       {"name":"Séance 2","prescriptions":[{"exerciseId":%d,"sets":2,"durationSeconds":30,"restSeconds":20}]}]}
          """.formatted(repsId,durationId));
    }
    JsonNode create(ObjectNode input) throws Exception {
        return mapper.readTree(mvc.perform(post("/programs").contentType("application/json").content(input.toString())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
    }
    JsonNode getProgram(long id) throws Exception {return mapper.readTree(mvc.perform(get("/programs/"+id)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");}
    ObjectNode editable(JsonNode detail) {
        JsonNode version=detail.get("version");ObjectNode form=mapper.createObjectNode();
        for(String key:new String[]{"name","description","objective"}) form.put(key,version.get(key).asText());
        ArrayNode sessions=form.putArray("sessions");
        for(JsonNode session:version.get("sessions")){
            ObjectNode s=sessions.addObject().put("name",session.get("name").asText());ArrayNode ps=s.putArray("prescriptions");
            for(JsonNode p:session.get("prescriptions")){
                ObjectNode n=ps.addObject();n.set("existingPrescriptionId",p.get("id"));
                for(String key:new String[]{"exerciseId","sets","repetitions","durationSeconds","restSeconds","targetLoadKg"}) n.set(key,p.get(key));
            }
        }
        return form;
    }
    ObjectNode updateBody(int revision,ObjectNode input){ObjectNode body=mapper.createObjectNode().put("expectedRevision",revision);body.set("program",input);return body;}
    @Test void createsMultipleSessionsAndFiltersPages() throws Exception {
        JsonNode p=create(input());assertThat(p.at("/version/sessions").size()).isEqualTo(2);
        assertThat(p.at("/version/sessions/0/prescriptions/0/exerciseName").asText()).isEqualTo("Squat");
        assertThat(p.at("/version/sessions/1/prescriptions/0/durationSeconds").asInt()).isEqualTo(30);
        mvc.perform(get("/programs").param("search","PROGRAMME").param("size","1")).andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.items[0].sessionCount").value(2));
        mvc.perform(get("/programs").param("size","51")).andExpect(status().isBadRequest());
    }
    @Test void rejectsInconsistentPrescriptionAndRollsBackWholeCreation() throws Exception {
        ObjectNode form=input();((ObjectNode)form.at("/sessions/1/prescriptions/0")).put("repetitions",5);
        mvc.perform(post("/programs").contentType("application/json").content(form.toString())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        for(String table:new String[]{"training_programs","training_program_versions","training_program_sessions","training_prescriptions"}) assertThat(jdbc.queryForObject("select count(*) from "+table,Long.class)).isZero();
        ((ObjectNode)form.at("/sessions/0/prescriptions/0")).put("sets",0);
        mvc.perform(post("/programs").contentType("application/json").content(form.toString())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errors").isNotEmpty());
        ((ArrayNode)form.get("sessions")).removeAll();
        mvc.perform(post("/programs").contentType("application/json").content(form.toString())).andExpect(status().isBadRequest());
    }
    @Test void createsImmutableRevisionAndRejectsStaleRevision() throws Exception {
        JsonNode p=create(input());long id=p.get("id").asLong();ObjectNode edit=editable(p);edit.put("name","Programme B");
        mvc.perform(put("/programs/"+id).contentType("application/json").content(updateBody(1,edit).toString())).andExpect(status().isOk()).andExpect(jsonPath("$.data.currentRevision").value(2));
        mvc.perform(get("/programs/"+id+"/revisions/1")).andExpect(jsonPath("$.data.version.name").value("Programme A")).andExpect(jsonPath("$.data.version.sessions[0].prescriptions[0].id").value(p.at("/version/sessions/0/prescriptions/0/id").asLong()));
        mvc.perform(put("/programs/"+id).contentType("application/json").content(updateBody(1,edit).toString())).andExpect(status().isConflict());
        mvc.perform(get("/programs/"+id+"/revisions")).andExpect(jsonPath("$.data.totalElements").value(2));
    }
    @Test void noOpDoesNotCreateRevision() throws Exception {
        JsonNode p=create(input());long id=p.get("id").asLong();
        mvc.perform(put("/programs/"+id).contentType("application/json").content(updateBody(1,editable(p)).toString())).andExpect(status().isOk()).andExpect(jsonPath("$.data.currentRevision").value(1));
    }
    @Test void duplicationUsesNewIdsAndIsIndependent() throws Exception {
        JsonNode source=create(input());long id=source.get("id").asLong();
        JsonNode copy=mapper.readTree(mvc.perform(post("/programs/"+id+"/duplicate")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
        assertThat(copy.get("id")).isNotEqualTo(source.get("id"));assertThat(copy.at("/version/id")).isNotEqualTo(source.at("/version/id"));
        assertThat(copy.at("/version/sessions/0/id")).isNotEqualTo(source.at("/version/sessions/0/id"));
        assertThat(copy.at("/version/sessions/0/prescriptions/0/id")).isNotEqualTo(source.at("/version/sessions/0/prescriptions/0/id"));
        ObjectNode edit=editable(copy).put("name","Copie modifiée");
        mvc.perform(put("/programs/"+copy.get("id").asLong()).contentType("application/json").content(updateBody(1,edit).toString())).andExpect(status().isOk());
        assertThat(getProgram(id).get("name").asText()).isEqualTo("Programme A");
    }
    @Test void isolatesOwnersForAllPrivateEndpoints() throws Exception {
        long id=create(input()).get("id").asLong();when(user.ownerReference()).thenReturn("bob");
        mvc.perform(get("/programs")).andExpect(jsonPath("$.data.totalElements").value(0));
        for(String suffix:new String[]{"","/revisions","/revisions/1"}) mvc.perform(get("/programs/"+id+suffix)).andExpect(status().isNotFound());
        mvc.perform(put("/programs/"+id).contentType("application/json").content(updateBody(1,input()).toString())).andExpect(status().isNotFound());
        mvc.perform(post("/programs/"+id+"/duplicate")).andExpect(status().isNotFound());
        for(String action:new String[]{"archive","restore"}) mvc.perform(patch("/programs/"+id+"/"+action).contentType("application/json").content("{\"expectedRevision\":1}")).andExpect(status().isNotFound());
        create(input());mvc.perform(get("/programs")).andExpect(jsonPath("$.data.totalElements").value(1));
        when(user.ownerReference()).thenReturn("alice");mvc.perform(get("/programs")).andExpect(jsonPath("$.data.totalElements").value(1));
    }
    @Test void archivedExerciseKeepsSnapshotAndCanBeRetainedButNotAdded() throws Exception {
        JsonNode p=create(input());long id=p.get("id").asLong();
        mvc.perform(put("/exercises/"+repsId).contentType("application/json").content("""
            {"name":"Nouveau nom","description":"Description","instructions":"Consignes","muscleGroup":"CORE","equipment":"DUMBBELLS","difficulty":"BEGINNER","measurementType":"DURATION"}
            """)).andExpect(status().isOk());
        mvc.perform(patch("/exercises/"+repsId+"/archive")).andExpect(status().isOk());
        JsonNode after=getProgram(id);assertThat(after.at("/version/sessions/0/prescriptions/0/exerciseArchived").asBoolean()).isTrue();
        assertThat(after.at("/version/sessions/0/prescriptions/0/exerciseName").asText()).isEqualTo("Squat");
        assertThat(after.at("/version/sessions/0/prescriptions/0/equipment").asText()).isEqualTo("NONE");
        ObjectNode edit=editable(after).put("name","Conservé");
        mvc.perform(put("/programs/"+id).contentType("application/json").content(updateBody(1,edit).toString())).andExpect(status().isOk()).andExpect(jsonPath("$.data.version.sessions[0].prescriptions[0].measurementType").value("REPETITIONS"));
        mvc.perform(post("/programs").contentType("application/json").content(input().toString())).andExpect(status().isBadRequest());
        mvc.perform(post("/programs/"+id+"/duplicate")).andExpect(status().isCreated());
        mvc.perform(get("/programs/"+id+"/revisions/1")).andExpect(jsonPath("$.data.version.name").value("Programme A"));
    }
    @Test void archivesAndRestoresWithRevisions() throws Exception {
        JsonNode p=create(input());long id=p.get("id").asLong();
        mvc.perform(patch("/programs/"+id+"/archive").contentType("application/json").content("{\"expectedRevision\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.currentRevision").value(2));
        mvc.perform(get("/programs")).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/programs").param("status","ARCHIVED")).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(put("/programs/"+id).contentType("application/json").content(updateBody(2,editable(p)).toString())).andExpect(status().isConflict());
        mvc.perform(patch("/programs/"+id+"/restore").contentType("application/json").content("{\"expectedRevision\":2}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.archived").value(false)).andExpect(jsonPath("$.data.currentRevision").value(3));
        mvc.perform(get("/programs/"+id+"/revisions/2")).andExpect(jsonPath("$.data.version.archived").value(true));
    }
    @Test void rollsBackWhenPersistenceFailsAfterSavingProgramHeader() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("test-only failure"))
            .when(versionRepository).saveAndFlush(org.mockito.ArgumentMatchers.any());
        mvc.perform(post("/programs").contentType("application/json").content(input().toString()))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Le service est temporairement indisponible. Réessayez plus tard."));
        for(String table:new String[]{"training_programs","training_program_versions","training_program_sessions","training_prescriptions"})
            assertThat(jdbc.queryForObject("select count(*) from "+table,Long.class)).isZero();
    }
    @Test void boundsPayloadAndRejectsInvalidValues() throws Exception {
        mvc.perform(post("/programs").servletPath("/programs").contentType("application/json").content(" ".repeat(1024*1024+1))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        ObjectNode fractional=input();((ObjectNode)fractional.at("/sessions/0/prescriptions/0")).put("sets",1.5);
        mvc.perform(post("/programs").contentType("application/json").content(fractional.toString())).andExpect(status().isBadRequest());
        ObjectNode form=input();((ObjectNode)form.at("/sessions/0/prescriptions/0")).put("targetLoadKg",-1);
        mvc.perform(post("/programs").contentType("application/json").content(form.toString())).andExpect(status().isBadRequest());
    }
}


