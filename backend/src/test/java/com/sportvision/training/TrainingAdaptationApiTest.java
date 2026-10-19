package com.sportvision.training;
import com.fasterxml.jackson.databind.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import com.sportvision.training.service.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.sportvision.SportVisionApplication.class, properties={"spring.datasource.url=jdbc:h2:mem:training_adaptation_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(username="training-test-user", roles="ADMIN")
class TrainingAdaptationApiTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;
    @MockBean GeminiClient gemini;@MockBean TrainingCurrentUserProvider user;
    JsonNode program;long id;String url;
    @BeforeEach void setup()throws Exception{
        for(String table:List.of("training_adaptation_proposals","training_workout_sets","training_workout_exercises","training_workouts","training_prescriptions","training_program_sessions","training_program_versions","training_programs","training_exercises"))jdbc.update("delete from "+table);
        when(user.ownerReference()).thenReturn("PRIVATE_OWNER");program=create("REPETITIONS");id=program.path("id").asLong();url="/api/training/ai/programs/"+id+"/adaptation";
        when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"adjustments\":[{\"slot\":0,\"option\":\"REDUCE_TARGET\"}]}");
    }
    JsonNode data(ResultActions result)throws Exception{return mapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");}
    JsonNode create(String type)throws Exception{
        long exercise=data(mvc.perform(post("/api/training/exercises").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("""
            {"name":"PRIVATE_EXERCISE","description":"PRIVATE_DESCRIPTION","instructions":"PRIVATE_INSTRUCTIONS","muscleGroup":"LEGS","equipment":"NONE","difficulty":"BEGINNER","measurementType":"%s"}
            """.formatted(type))).andExpect(status().isCreated())).path("id").asLong();
        return data(mvc.perform(post("/api/training/programs").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("""
            {"name":"PRIVATE_PROGRAM","description":"PRIVATE_DESCRIPTION","objective":"PRIVATE_OBJECTIVE","sessions":[{"name":"PRIVATE_SESSION","prescriptions":[{"exerciseId":%d,"sets":3,%s,"restSeconds":30,"targetLoadKg":5}]}]}
            """.formatted(exercise,type.equals("REPETITIONS")?"\"repetitions\":10":"\"durationSeconds\":30"))).andExpect(status().isCreated()));
    }
    JsonNode workout(JsonNode p,int value,boolean all,Integer load)throws Exception{
        var w=data(mvc.perform(post("/api/training/workouts").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("{\"programId\":"+p.path("id")+",\"revision\":"+p.path("currentRevision")+",\"sessionId\":"+p.at("/version/sessions/0/id")+"}")).andExpect(status().isOk()));
        String field=p.at("/version/sessions/0/prescriptions/0/measurementType").asText().equals("REPETITIONS")?"repetitions":"durationSeconds";
        int index=0;for(var set:w.at("/exercises/0/sets")){
            if(index++>0&&!all)break;
            mvc.perform(put("/api/training/workouts/"+w.path("id").asLong()+"/sets/"+set.path("id").asLong()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("{\""+field+"\":"+value+(load==null?"":",\"loadKg\":"+load)+"}")).andExpect(status().isOk());
        }
        return data(mvc.perform(post("/api/training/workouts/"+w.path("id").asLong()+"/complete").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("{\"confirmSkipPending\":true,\"comment\":\"PRIVATE_COMMENT\"}")).andExpect(status().isOk()));
    }
    void history()throws Exception{workout(program,8,false,4);workout(program,7,false,4);}
    ResultActions propose()throws Exception{return mvc.perform(post(url).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("{\"expectedRevision\":1}"));}
    ResultActions confirm(String token,int revision)throws Exception{return mvc.perform(post(url+"/confirm").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("{\"expectedRevision\":"+revision+",\"proposalId\":\""+token+"\"}"));}
    @Test void insufficientDataNeverCallsGemini()throws Exception{
        mvc.perform(get(url).param("expectedRevision","1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.eligible").value(false));
        propose().andExpect(status().isUnprocessableEntity());workout(program,8,true,4);propose().andExpect(status().isUnprocessableEntity());verifyNoInteractions(gemini);
    }
    @Test void computesMetricsAndSendsOnlyAnonymousStructuredData()throws Exception{
        history();var preview=data(propose().andExpect(status().isOk()));
        assertThat(preview.at("/report/items/0/metrics/done").asInt()).isEqualTo(2);
        assertThat(preview.at("/report/items/0/metrics/skipped").asInt()).isEqualTo(4);
        assertThat(preview.at("/report/items/0/metrics/difference").asInt()).isEqualTo(-5);
        assertThat(preview.at("/changes/0/after/repetitions").asInt()).isEqualTo(9);
        var payload=ArgumentCaptor.forClass(String.class);verify(gemini).generate(anyString(),payload.capture(),any());
        assertThat(payload.getValue()).doesNotContain("PRIVATE_","prescriptionId","ownerReference","programId","exerciseId","comment","objective");
        var input=mapper.readTree(payload.getValue()).get(0);Set<String> fields=new HashSet<>();input.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("slot","measurementType","prescription","metrics","allowed");
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(1);
    }
    @Test void confirmationCreatesRevisionPreservesHistoryAndRejectsReplay()throws Exception{
        history();var old=data(mvc.perform(get("/api/training/programs/"+id+"/revisions/1")).andExpect(status().isOk()));
        long wid=jdbc.queryForObject("select min(id) from training_workouts",Long.class);
        var snapshot=data(mvc.perform(get("/api/training/workouts/"+wid)).andExpect(status().isOk()));
        String token=data(propose().andExpect(status().isOk())).path("proposalId").asText();
        var updated=data(confirm(token,1).andExpect(status().isOk()));
        assertThat(updated.path("currentRevision").asInt()).isEqualTo(2);
        assertThat(updated.at("/version/sessions/0/prescriptions/0/repetitions").asInt()).isEqualTo(9);
        assertThat(updated.at("/version/sessions/0/prescriptions/0/exerciseId")).isEqualTo(old.at("/version/sessions/0/prescriptions/0/exerciseId"));
        assertThat(data(mvc.perform(get("/api/training/programs/"+id+"/revisions/1")).andExpect(status().isOk())).path("version")).isEqualTo(old.path("version"));
        assertThat(data(mvc.perform(get("/api/training/workouts/"+wid)).andExpect(status().isOk()))).isEqualTo(snapshot);
        confirm(token,1).andExpect(status().isConflict());confirm(token,2).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(2);
        mvc.perform(get(url).param("expectedRevision","2")).andExpect(status().isOk()).andExpect(jsonPath("$.data.completedWorkouts").value(0));
    }
    @Test void rejectsInvalidRecommendationsWithoutPersistingProposal()throws Exception{
        history();for(String raw:List.of("not json","null","{}","{\"adjustments\":null}",
            "{\"adjustments\":[{\"slot\":99,\"option\":\"REDUCE_TARGET\"}]}","{\"adjustments\":[{\"slot\":0,\"option\":\"INCREASE_TARGET\"}]}",
            "{\"adjustments\":[{\"slot\":null,\"option\":\"REDUCE_TARGET\"}]}","{\"adjustments\":[{\"slot\":0.5,\"option\":\"REDUCE_TARGET\"}]}",
            "{\"adjustments\":[{\"slot\":0,\"option\":\"REDUCE_TARGET\",\"repetitions\":999}]}",
            "{\"adjustments\":[{\"slot\":0,\"option\":\"REDUCE_TARGET\"},{\"slot\":0,\"option\":\"MORE_REST\"}]}")){
            when(gemini.generate(anyString(),anyString(),any())).thenReturn(raw);propose().andExpect(status().isBadGateway());
        }
        assertThat(jdbc.queryForObject("select count(*) from training_adaptation_proposals",Integer.class)).isZero();
    }
    @Test void rejectsForgedPayloadUnknownTokenAndWrongOwner()throws Exception{
        history();String token=data(propose().andExpect(status().isOk())).path("proposalId").asText();
        mvc.perform(post(url+"/confirm").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("{\"expectedRevision\":1,\"proposalId\":\""+token+"\",\"changes\":[]}")).andExpect(status().isBadRequest());
        confirm(UUID.randomUUID().toString(),1).andExpect(status().isNotFound());
        when(user.ownerReference()).thenReturn("OTHER_OWNER");confirm(token,1).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(1);
    }
    @Test void newHistoryInvalidatesProposal()throws Exception{
        history();String token=data(propose().andExpect(status().isOk())).path("proposalId").asText();
        workout(program,10,true,5);confirm(token,1).andExpect(status().isConflict());
    }
    @Test void expiredProposalAndStaleRevisionAreRejected()throws Exception{
        history();String token=data(propose().andExpect(status().isOk())).path("proposalId").asText();
        jdbc.update("update training_adaptation_proposals set expires_at=?",java.sql.Timestamp.valueOf("2000-01-01 00:00:00"));
        confirm(token,1).andExpect(status().isConflict());
        mvc.perform(post(url).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("training-test-user").roles("ADMIN")).contentType("application/json").content("{\"expectedRevision\":2}")).andExpect(status().isConflict());
        verify(gemini,times(1)).generate(anyString(),anyString(),any());
    }
    @Test void durationsAndObservedLoadsAreBounded()throws Exception{
        program=create("DURATION");id=program.path("id").asLong();url="/api/training/ai/programs/"+id+"/adaptation";
        workout(program,30,true,6);workout(program,35,true,6);
        when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"adjustments\":[{\"slot\":0,\"option\":\"INCREASE_TARGET\"}]}");
        var p=data(propose().andExpect(status().isOk()));assertThat(p.at("/changes/0/after/durationSeconds").asInt()).isEqualTo(33);assertThat(p.at("/changes/0/after/repetitions").isNull()).isTrue();
        when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"adjustments\":[{\"slot\":0,\"option\":\"OBSERVED_LOAD\"}]}");
        var load=data(propose().andExpect(status().isOk()));assertThat(load.at("/changes/0/after/targetLoadKg").decimalValue()).isEqualByComparingTo("5.25");
        confirm(load.path("proposalId").asText(),1).andExpect(status().isOk());
    }
    @Test void missingLoadsNeverInventLoadProgression()throws Exception{
        workout(program,10,true,null);workout(program,10,true,null);
        propose().andExpect(status().isUnprocessableEntity());verifyNoInteractions(gemini);
    }
    @Test void emptyProposalAndProviderFailureLeaveProgramUnchanged()throws Exception{
        history();when(gemini.generate(anyString(),anyString(),any())).thenReturn("{\"adjustments\":[]}");
        propose().andExpect(status().isOk()).andExpect(jsonPath("$.data.proposalId").doesNotExist()).andExpect(jsonPath("$.data.changes").isEmpty());
        when(gemini.generate(anyString(),anyString(),any())).thenThrow(new TrainingAiException(503,"NETWORK","Service indisponible."));
        propose().andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(1);
    }
    @Test void concurrentConfirmationsCreateOnlyOneRevision()throws Exception{
        history();String token=data(propose().andExpect(status().isOk())).path("proposalId").asText();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var gate=new java.util.concurrent.CountDownLatch(1);
        try{
            java.util.concurrent.Callable<Integer> call=()->{gate.await();return confirm(token,1).andReturn().getResponse().getStatus();};
            var a=pool.submit(call);var b=pool.submit(call);gate.countDown();
            assertThat(List.of(a.get(30,java.util.concurrent.TimeUnit.SECONDS),b.get(30,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("select count(*) from training_program_versions",Integer.class)).isEqualTo(2);
    }
}
