package com.sportvision.training;
import com.fasterxml.jackson.databind.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.service.GeminiClient;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:training_alternative_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc
class TrainingAlternativeApiTest {
    private static final String PRIVATE_OBJECTIVE="OBJECTIF_PRIVE_TEST : personne fictive, dossier personnel confidentiel.";
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;
    @MockBean GeminiClient gemini;@MockBean TrainingCurrentUserProvider user;
    long source,candidate,other,program,prescription;String request;JsonNode oldVersion;
    @BeforeEach void setup()throws Exception{
        for(String table:new String[]{"training_prescriptions","training_program_sessions","training_program_versions","training_programs","training_exercises"})jdbc.update("delete from "+table);
        when(user.ownerReference()).thenReturn("alternative-owner");
        source=exercise("Source","LEGS","DUMBBELLS");candidate=exercise("Alternative","LEGS","NONE");other=exercise("Autre","CORE","NONE");
        String input="""
            {"name":"Programme isolé","description":"Description","objective":"%s","sessions":[{"name":"Séance","prescriptions":[
            {"exerciseId":%d,"sets":3,"repetitions":12,"restSeconds":60,"targetLoadKg":5},
            {"exerciseId":%d,"sets":2,"repetitions":8,"restSeconds":45,"targetLoadKg":2}]}]}
            """.formatted(PRIVATE_OBJECTIVE,source,other);
        var result=mapper.readTree(mvc.perform(post("/programs").contentType("application/json").content(input)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data");
        program=result.path("id").asLong();
        // Comparer deux lectures persistées (précision SQL des instants et des décimaux).
        oldVersion=mapper.readTree(mvc.perform(get("/programs/"+program+"/revisions/1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).at("/data/version");
        prescription=oldVersion.at("/sessions/0/prescriptions/0/id").asLong();
        request="""
            {"expectedRevision":1,"prescriptionId":%d,"motive":"EQUIPMENT_UNAVAILABLE","level":"BEGINNER","equipment":["NONE"]}
            """.formatted(prescription);
    }
    long exercise(String name,String muscle,String equipment)throws Exception{
        String input="""
            {"name":"%s","description":"Description","instructions":"Consignes","muscleGroup":"%s","equipment":"%s","difficulty":"BEGINNER","measurementType":"REPETITIONS"}
            """.formatted(name,muscle,equipment);
        return mapper.readTree(mvc.perform(post("/exercises").contentType("application/json").content(input)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).at("/data/id").asLong();
    }
    @Test void sendsOnlyStructuredCharacteristicsWithoutPrivateObjectiveOrFreeText()throws Exception{
        when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"exerciseId\":"+candidate+",\"reason\":\"Même groupe musculaire, sans matériel.\"}");
        mvc.perform(post("/ai/programs/"+program+"/alternative").contentType("application/json").content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.alternative.name").value("Alternative"))
            .andExpect(jsonPath("$.data.current.exerciseName").value("Source"));
        var instructions=ArgumentCaptor.forClass(String.class);
        var payload=ArgumentCaptor.forClass(String.class);
        var schema=ArgumentCaptor.forClass(JsonNode.class);
        verify(gemini,times(1)).generate(instructions.capture(),payload.capture(),schema.capture());
        assertThat(instructions.getValue()+payload.getValue()+schema.getValue().toString())
            .doesNotContain(PRIVATE_OBJECTIVE,"OBJECTIF_PRIVE_TEST","alternative-owner","Programme isolé","Description","Consignes","Séance");
        var data=mapper.readTree(payload.getValue());
        java.util.Set<String> fields=new java.util.HashSet<>();data.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("current","motive","level","equipment","catalogue");
        var expected=mapper.readTree("""
            {"current":{"id":%d,"muscleGroup":"LEGS","equipment":"DUMBBELLS","measurementType":"REPETITIONS"},
             "motive":"EQUIPMENT_UNAVAILABLE","level":"BEGINNER","equipment":["NONE"],
             "catalogue":[{"id":%d,"muscleGroup":"LEGS","equipment":"NONE","measurementType":"REPETITIONS"}]}
            """.formatted(source,candidate));
        assertThat(data).isEqualTo(expected);
        assertThat(oldVersion.path("objective").asText()).isEqualTo(PRIVATE_OBJECTIVE);
        var persisted=mapper.readTree(mvc.perform(get("/programs/"+program)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).path("data");
        assertThat(persisted.path("currentRevision").asInt()).isEqualTo(1);
        assertThat(persisted.path("version")).isEqualTo(oldVersion);
    }
    @Test void rejectsAlternativeOutsideAllowedCandidates()throws Exception{
        when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"exerciseId\":"+other+",\"reason\":\"Choix invalide\"}");
        mvc.perform(post("/ai/programs/"+program+"/alternative").contentType("application/json").content(request))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.data.code").value("INVALID_ALTERNATIVE"));
        mvc.perform(post("/ai/programs/"+program+"/replacement").contentType("application/json").content(confirmation(other)))
            .andExpect(status().isBadRequest());
        verify(gemini,times(1)).generate(anyString(),argThat(data->!data.contains("alternative-owner")),any());
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(1);
    }
    @Test void confirmsExactlyOneRevisionPreservesOldSnapshotAndRejectsStaleConfirmation()throws Exception{
        when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"exerciseId\":"+candidate+",\"reason\":\"Même groupe musculaire, sans matériel.\"}");
        mvc.perform(post("/ai/programs/"+program+"/alternative").contentType("application/json").content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.alternative.name").value("Alternative"));
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(1);
        var updated=mapper.readTree(mvc.perform(post("/ai/programs/"+program+"/replacement").contentType("application/json").content(confirmation(candidate)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.currentRevision").value(2)).andReturn().getResponse().getContentAsString()).path("data");
        updated=mapper.readTree(mvc.perform(get("/programs/"+program)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        var replacement=updated.at("/version/sessions/0/prescriptions/0");
        assertThat(updated.at("/version/objective").asText()).isEqualTo(PRIVATE_OBJECTIVE);
        assertThat(replacement.path("exerciseId").asLong()).isEqualTo(candidate);
        assertThat(replacement.path("exerciseName").asText()).isEqualTo("Alternative");
        assertThat(replacement.path("equipment").asText()).isEqualTo("NONE");
        assertThat(replacement.path("targetLoadKg").isNull()).isTrue();
        for(String field:new String[]{"sets","repetitions","durationSeconds","restSeconds","measurementType","position"})
            assertThat(replacement.path(field)).isEqualTo(oldVersion.at("/sessions/0/prescriptions/0").path(field));
        var unchanged=(com.fasterxml.jackson.databind.node.ObjectNode)updated.at("/version/sessions/0/prescriptions/1").deepCopy();unchanged.remove("id");
        var before=(com.fasterxml.jackson.databind.node.ObjectNode)oldVersion.at("/sessions/0/prescriptions/1").deepCopy();before.remove("id");assertThat(unchanged).isEqualTo(before);
        var historical=mapper.readTree(mvc.perform(get("/programs/"+program+"/revisions/1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).at("/data/version");
        assertThat(historical).isEqualTo(oldVersion);
        mvc.perform(post("/ai/programs/"+program+"/replacement").contentType("application/json").content(confirmation(candidate))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(2);
        verify(gemini,times(1)).generate(anyString(),anyString(),any());
    }
    String confirmation(long id){return "{\"request\":"+request+",\"exerciseId\":"+id+"}";}
}
