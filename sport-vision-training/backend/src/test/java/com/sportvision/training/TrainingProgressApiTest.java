package com.sportvision.training;
import com.fasterxml.jackson.databind.*;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import java.time.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:training_progress_tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","training.demo.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc
class TrainingProgressApiTest {
    @TestConfiguration static class TimeConfig {
        @Bean @Primary Clock fixedClock(){return Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"),ZoneOffset.UTC);}
    }
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;
    @MockBean TrainingCurrentUserProvider user;
    JsonNode data(ResultActions response)throws Exception{return mapper.readTree(response.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");}
    JsonNode postJson(String url,String json)throws Exception{return data(mvc.perform(post(url).contentType("application/json").content(json)).andExpect(status().is2xxSuccessful()));}
    JsonNode program(long exercise)throws Exception{return postJson("/programs","""
        {"name":"Programme test","description":"Description","objective":"Objectif","sessions":[{"name":"Séance test","prescriptions":[{"exerciseId":%d,"sets":3,"repetitions":10,"restSeconds":30}]}]}
        """.formatted(exercise));}
    long workout(JsonNode program,int[] reps,String[] loads,String action,String ended,int elapsed)throws Exception{
        JsonNode w=postJson("/workouts","{\"programId\":"+program.get("id")+",\"revision\":1,\"sessionId\":"+program.at("/version/sessions/0/id")+"}");
        long id=w.get("id").asLong();
        for(int i=0;i<reps.length;i++)mvc.perform(put("/workouts/"+id+"/sets/"+w.at("/exercises/0/sets/"+i+"/id").asLong()).contentType("application/json")
            .content("{\"repetitions\":"+reps[i]+",\"loadKg\":"+loads[i]+"}")).andExpect(status().isOk());
        if(action!=null)postJson("/workouts/"+id+"/"+action,"{\"confirmSkipPending\":true}");
        if(ended!=null){Instant end=Instant.parse(ended);jdbc.update("update training_workouts set started_at=?,ended_at=? where id=?",Timestamp.from(end.minusSeconds(elapsed)),Timestamp.from(end),id);}
        return id;
    }
    @Test void aggregatesOnlyOwnedCompletedPerformancesWithTunisBoundariesAndSnapshotTypes()throws Exception{
        when(user.ownerReference()).thenReturn("alice");
        long ex=postJson("/exercises","""
            {"name":"Squat historique","description":"Description","instructions":"Consignes","muscleGroup":"LEGS","equipment":"NONE","difficulty":"BEGINNER","measurementType":"REPETITIONS"}
            """).get("id").asLong();
        JsonNode p=program(ex);
        long first=workout(p,new int[]{8,12},new String[]{"null","0"},"complete","2026-09-28T23:00:00Z",600);
        long second=workout(p,new int[]{5},new String[]{"12.5"},"complete","2026-10-04T23:30:00Z",120);
        jdbc.update("update training_workout_exercises set name='Squat renommé' where workout_id=?",second);
        long timed=workout(p,new int[]{30,40},new String[]{"null","null"},"complete","2026-10-05T10:00:00Z",90);
        jdbc.update("update training_workout_exercises set measurement_type='DURATION',measurement_label='Durée',name='Maintien historique' where workout_id=?",timed);
        jdbc.update("update training_workout_sets set duration_seconds=repetitions,repetitions=null where exercise_id in (select id from training_workout_exercises where workout_id=?)",timed);
        workout(p,new int[]{99},new String[]{"50"},"abandon","2026-10-05T11:00:00Z",1000);
        workout(p,new int[]{100},new String[]{"100"},"complete","2026-09-28T22:59:59.999999Z",1000);
        workout(p,new int[]{100},new String[]{"100"},"complete","2026-10-05T12:00:00Z",1000);
        workout(p,new int[]{100},new String[]{"100"},null,null,0);
        when(user.ownerReference()).thenReturn("bob");
        workout(program(ex),new int[]{200},new String[]{"200"},"complete","2026-10-05T11:00:00Z",2000);
        jdbc.update("update training_exercises set archived=true,name='Catalogue modifié' where id=?",ex);
        when(user.ownerReference()).thenReturn("alice");
        JsonNode result=data(mvc.perform(get("/progress").param("days","7").param("owner","bob")).andExpect(status().isOk()));
        assertThat(result.at("/period/fromInclusive").asText()).isEqualTo("2026-09-28T23:00:00Z");
        assertThat(result.at("/period/toExclusive").asText()).isEqualTo("2026-10-05T12:00:00Z");
        assertThat(result.at("/period/timeZone").asText()).isEqualTo("Africa/Tunis");
        assertThat(result.at("/indicators/completedWorkouts").asInt()).isEqualTo(3);
        assertThat(result.at("/indicators/abandonedWorkouts").asInt()).isEqualTo(1);
        assertThat(result.at("/indicators/doneSets").asInt()).isEqualTo(5);
        assertThat(result.at("/indicators/elapsedSeconds").asInt()).isEqualTo(810);
        assertThat(result.get("days").size()).isEqualTo(7);
        assertThat(result.at("/days/0/completedWorkouts").asInt()).isEqualTo(1);
        assertThat(result.at("/days/6/completedWorkouts").asInt()).isEqualTo(2);
        assertThat(result.get("exercises").size()).isEqualTo(2);
        JsonNode reps=data(mvc.perform(get("/progress/exercises/"+ex).param("days","7").param("measurementType","REPETITIONS")).andExpect(status().isOk()));
        assertThat(reps.get("points").size()).isEqualTo(2);
        assertThat(reps.at("/points/0/workoutId").asLong()).isEqualTo(first);
        assertThat(reps.at("/points/0/doneSets").asInt()).isEqualTo(2);
        assertThat(reps.at("/points/0/totalRepetitions").asInt()).isEqualTo(20);
        assertThat(reps.at("/points/0/maxLoadKg").decimalValue()).isEqualByComparingTo("0");
        assertThat(reps.at("/points/0/effortSeconds").isNull()).isTrue();
        assertThat(reps.at("/points/0/historicalNames/0").asText()).isEqualTo("Squat historique");
        assertThat(reps.at("/points/1/totalRepetitions").asInt()).isEqualTo(5);
        assertThat(reps.at("/points/1/date").asText()).isEqualTo("2026-10-05");
        assertThat(reps.at("/points/1/maxLoadKg").decimalValue()).isEqualByComparingTo("12.5");
        assertThat(reps.at("/points/1/historicalNames/0").asText()).isEqualTo("Squat renommé");
        JsonNode duration=data(mvc.perform(get("/progress/exercises/"+ex).param("days","7").param("measurementType","DURATION")).andExpect(status().isOk()));
        assertThat(duration.at("/points/0/effortSeconds").asInt()).isEqualTo(70);
        assertThat(duration.at("/points/0/totalRepetitions").isNull()).isTrue();
        assertThat(duration.at("/points/0/maxLoadKg").isNull()).isTrue();
        mvc.perform(get("/progress").param("days","14")).andExpect(status().isBadRequest());
        mvc.perform(get("/progress")).andExpect(jsonPath("$.data.period.days").value(30)).andExpect(jsonPath("$.data.indicators.completedWorkouts").value(4));
        mvc.perform(get("/progress").param("days","90")).andExpect(jsonPath("$.data.days.length()").value(90));
        when(user.ownerReference()).thenReturn("bob");
        mvc.perform(get("/progress").param("days","7")).andExpect(jsonPath("$.data.indicators.completedWorkouts").value(1)).andExpect(jsonPath("$.data.indicators.doneSets").value(1));
        when(user.ownerReference()).thenReturn("empty-owner");
        mvc.perform(get("/progress")).andExpect(jsonPath("$.data.indicators.completedWorkouts").value(0)).andExpect(jsonPath("$.data.exercises").isEmpty());
    }
}
