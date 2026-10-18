package com.sportvision.training;
import com.fasterxml.jackson.databind.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.service.GeminiClient;
import com.sportvision.training.repository.TrainingAiConfirmationRepository;
import org.springframework.boot.test.mock.mockito.SpyBean;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.sportvision.SportVisionApplication.class, properties={"spring.datasource.url=jdbc:h2:mem:training_ai_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc
class TrainingAiApiTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;
    @MockBean GeminiClient gemini;@MockBean TrainingCurrentUserProvider user;
    @SpyBean TrainingAiConfirmationRepository confirmations;
    long[] ids=new long[3];
    final String preferences="{\"objective\":\"GENERAL_FITNESS\",\"level\":\"BEGINNER\",\"sessionCount\":1,\"equipment\":[\"NONE\"]}";
    @BeforeEach void setup()throws Exception{
        for(String table:new String[]{"training_ai_confirmations","training_prescriptions","training_program_sessions","training_program_versions","training_programs","training_exercises"})jdbc.update("delete from "+table);
        when(user.ownerReference()).thenReturn("owner-ai-test");
        for(int i=0;i<3;i++){
            String input="""
                {"name":"Exercice serveur %d","description":"Description","instructions":"Consignes","muscleGroup":"LEGS","equipment":"NONE","difficulty":"BEGINNER","measurementType":"REPETITIONS"}
                """.formatted(i+1);
            ids[i]=mapper.readTree(mvc.perform(post("/api/training/exercises").contentType("application/json").content(input)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).at("/data/id").asLong();
        }
    }
    String proposal(long first){
        return """
            {"name":"Programme proposé","description":"Un programme varié.","justification":"Les exercices utilisent le poids du corps.",
            "sessions":[{"name":"Séance découverte","exercises":[
            {"exerciseId":%d,"measurementType":"REPETITIONS","sets":2,"repetitions":8,"durationSeconds":null,"restSeconds":45},
            {"exerciseId":%d,"measurementType":"REPETITIONS","sets":2,"repetitions":10,"durationSeconds":null,"restSeconds":45},
            {"exerciseId":%d,"measurementType":"REPETITIONS","sets":2,"repetitions":12,"durationSeconds":null,"restSeconds":45}]}]}
            """.formatted(first,ids[1],ids[2]);
    }
    @Test void acceptsValidSimulatedProposalWithoutSavingUntilConfirmation()throws Exception{
        String generated=proposal(ids[0]);when(gemini.generate(anyString(),anyString(),any())).thenReturn(generated);
        mvc.perform(post("/api/training/ai/generate").contentType("application/json").content(preferences))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.proposal.sessions.length()").value(1))
            .andExpect(jsonPath("$.data.catalogue[0].name").value("Exercice serveur 1"));
        assertThat(jdbc.queryForObject("select count(*) from training_programs",Integer.class)).isZero();
        verify(gemini).generate(contains("DONNÉES"),argThat(data->!data.contains("owner-ai-test")&&data.contains("catalogue")),any());
        mvc.perform(post("/api/training/ai/programs").contentType("application/json").content(confirmation(UUID.randomUUID(),generated)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.currentRevision").value(1))
            .andExpect(jsonPath("$.data.version.sessions[0].prescriptions[0].exerciseName").value("Exercice serveur 1"))
            .andExpect(jsonPath("$.data.version.sessions[0].prescriptions[0].targetLoadKg").doesNotExist());
        assertThat(jdbc.queryForObject("select owner_reference from training_programs",String.class)).isEqualTo("owner-ai-test");
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(1);
    }
    @Test void rejectsUnauthorizedExerciseInSimulatedProposalAndOnSave()throws Exception{
        String generated=proposal(999999);when(gemini.generate(anyString(),anyString(),any())).thenReturn(generated);
        mvc.perform(post("/api/training/ai/generate").contentType("application/json").content(preferences))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.data.code").value("INVALID_PROPOSAL"));
        mvc.perform(post("/api/training/ai/programs").contentType("application/json").content(confirmation(UUID.randomUUID(),generated)))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from training_programs",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isZero();
    }
    @Test void acceptsOmittedUnusedMeasureButStillRejectsMissingRequiredMeasureAndInvalidBounds()throws Exception{
        String generated=proposal(ids[0]).replace("\"durationSeconds\":null,","");
        when(gemini.generate(anyString(),anyString(),any())).thenReturn(generated);
        mvc.perform(post("/api/training/ai/generate").contentType("application/json").content(preferences)).andExpect(status().isOk());
        for(String invalid:new String[]{generated.replace("\"repetitions\":8,",""),
            generated.replace("\"sets\":2","\"sets\":101"),generated.replace("\"restSeconds\":45","\"restSeconds\":null"),
            generated.replace("\"measurementType\":\"REPETITIONS\"","\"measurementType\":\"DURATION\"")}){
            when(gemini.generate(anyString(),anyString(),any())).thenReturn(invalid);
            mvc.perform(post("/api/training/ai/generate").contentType("application/json").content(preferences))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.data.code").value("INVALID_PROPOSAL"));
        }
        assertThat(jdbc.queryForObject("select count(*) from training_programs",Integer.class)).isZero();
    }
    String confirmation(UUID key,String proposal){
        return "{\"idempotencyKey\":\""+key+"\",\"preferences\":"+preferences+",\"proposal\":"+proposal+"}";
    }
    JsonNode confirm(String body)throws Exception{
        return mapper.readTree(mvc.perform(post("/api/training/ai/programs").contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data");
    }
    void counts(int expected){
        for(String table:new String[]{"training_programs","training_program_versions","training_ai_confirmations"})
            assertThat(jdbc.queryForObject("select count(*) from "+table,Integer.class)).as(table).isEqualTo(expected);
    }
    @Test void repeatsConfirmationWithoutDuplicateEvenAfterCatalogueChanges()throws Exception{
        String body=confirmation(UUID.randomUUID(),proposal(ids[0]));
        long id=confirm(body).path("id").asLong();counts(1);
        // La répétition lit la confirmation persistée et ne revalide pas un catalogue devenu différent.
        jdbc.update("update training_exercises set archived=true where id=?",ids[0]);
        assertThat(confirm(body).path("id").asLong()).isEqualTo(id);
        counts(1);verifyNoInteractions(gemini);
    }
    @Test void rejectsChangedContentButAllowsNewKey()throws Exception{
        UUID key=UUID.randomUUID();String generated=proposal(ids[0]);
        long first=confirm(confirmation(key,generated)).path("id").asLong();
        for(String changed:new String[]{confirmation(key,generated.replace("Programme proposé","Autre programme")),
            confirmation(key,generated).replace("GENERAL_FITNESS","STRENGTH")}){
            mvc.perform(post("/api/training/ai/programs").contentType("application/json").content(changed))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.data.code").value("IDEMPOTENCY_CONFLICT"));
        }
        counts(1);
        assertThat(confirm(confirmation(UUID.randomUUID(),generated)).path("id").asLong()).isNotEqualTo(first);
        counts(2);
    }
    @Test void concurrentConfirmationsCommitOnlyOneProgram()throws Exception{
        String body=confirmation(UUID.randomUUID(),proposal(ids[0]));
        var ready=new CountDownLatch(6);var start=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(6);
        try{
            var futures=new java.util.ArrayList<Future<Long>>();
            for(int i=0;i<6;i++)futures.add(pool.submit(()->{
                ready.countDown();if(!start.await(10,TimeUnit.SECONDS))throw new AssertionError("Départ concurrent expiré");
                return confirm(body).path("id").asLong();
            }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();start.countDown();
            var returned=new java.util.HashSet<Long>();
            for(var future:futures)returned.add(future.get(30,TimeUnit.SECONDS));
            assertThat(returned).hasSize(1);counts(1);verifyNoInteractions(gemini);
        }finally{start.countDown();pool.shutdownNow();pool.awaitTermination(10,TimeUnit.SECONDS);}
    }
    @Test void scopesKeysToServerOwner()throws Exception{
        String body=confirmation(UUID.randomUUID(),proposal(ids[0]));
        long first=confirm(body).path("id").asLong();
        when(user.ownerReference()).thenReturn("different-owner");
        long second=confirm(body).path("id").asLong();
        assertThat(second).isNotEqualTo(first);
        assertThat(confirm(body).path("id").asLong()).isEqualTo(second);
        when(user.ownerReference()).thenReturn("owner-ai-test");
        assertThat(confirm(body).path("id").asLong()).isEqualTo(first);counts(2);
    }
    @Test void rollsBackProgramIfConfirmationCannotBeStoredThenAllowsRetry()throws Exception{
        String body=confirmation(UUID.randomUUID(),proposal(ids[0]));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("Échec simulé de confirmation"))
            .when(confirmations).saveAndFlush(any());
        mvc.perform(post("/api/training/ai/programs").contentType("application/json").content(body)).andExpect(status().isInternalServerError());
        counts(0);
        reset(confirmations);
        confirm(body);counts(1);
    }
    @Test void requiresValidConfirmationKey()throws Exception{
        for(String key:new String[]{"null","\"invalid\""}){
            var body=mapper.readTree(confirmation(UUID.randomUUID(),proposal(ids[0])));
            ((com.fasterxml.jackson.databind.node.ObjectNode)body).set("idempotencyKey",mapper.readTree(key));
            mvc.perform(post("/api/training/ai/programs").contentType("application/json").content(body.toString())).andExpect(status().isBadRequest());
        }
        counts(0);verifyNoInteractions(gemini);
    }
}
