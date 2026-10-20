package com.sportvision.training.bootstrap;

import java.sql.Timestamp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Personal, independent copies of the existing local fixture, never of another user's data. */
@Component
@Profile("local-demo & !mysql & !production")
@ConditionalOnProperty(name="training.dataset.enabled", havingValue="true")
public class TrainingPersonalDemoInitializer {
    private static final String TEMPLATE_OWNER="training-local-demo";
    private static final String TEMPLATE_MARK="integration-populated-v1";
    private static final ZoneId ZONE=ZoneId.of("Africa/Tunis");
    private final JdbcTemplate sql;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public TrainingPersonalDemoInitializer(JdbcTemplate sql, PlatformTransactionManager manager, Clock clock) {
        this.sql=sql; this.clock=clock;
        transaction=new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void prepare(String owner) {
        if (owner==null || !owner.matches("training-user:[0-9]+")) return;
        String marker="personal-demo-v1:"+owner;
        if (marked(marker)) return;
        transaction.executeWithoutResult(status -> {
            String url=sql.execute((org.springframework.jdbc.core.ConnectionCallback<String>) c -> c.getMetaData().getURL());
            if (url==null || !url.startsWith("jdbc:h2:")) throw new IllegalStateException("Training local fixtures require H2");
            // A durable existing row serializes first access, also between separate application processes.
            var template=sql.queryForList("select id from training_demo_seeds where id=? for update",TEMPLATE_MARK);
            if (template.isEmpty() || marked(marker)) return;
            Set<String> expectedNames=templateNames();
            List<Map<String,Object>> source=new ArrayList<>(); Set<String> foundNames=new HashSet<>();
            for (var candidate:rows("select * from training_programs where owner_reference=? order by id",TEMPLATE_OWNER)) {
                var first=rows("select name from training_program_versions where program_id=? and revision=1",id(candidate));
                if (first.isEmpty() || !expectedNames.contains(first.get(0).get("name"))) continue;
                if (!foundNames.add((String)first.get(0).get("name"))) throw new IllegalStateException("Ambiguous shared Training fixture; nothing copied");
                source.add(candidate);
            }
            if (source.isEmpty()) return; // Do not manufacture data when the shared initializer intentionally skipped.
            if (!foundNames.equals(expectedNames)) throw new IllegalStateException("Incomplete shared Training fixture; nothing copied");
            Set<Long> sourceIds=new HashSet<>(); source.forEach(p -> sourceIds.add(id(p)));
            var history=rows("select * from training_workouts where owner_reference=? and status='COMPLETED' order by id",TEMPLATE_OWNER)
                .stream().filter(w -> sourceIds.contains(((Number)w.get("program_id")).longValue())).toList();
            Instant latest=history.stream().map(r -> instant(r.get("ended_at"))).max(Instant::compareTo).orElse(clock.instant());
            long days=ChronoUnit.DAYS.between(latest.atZone(ZONE).toLocalDate(),LocalDate.now(clock.withZone(ZONE)));
            Map<Long,Long> programmes=new HashMap<>(), versions=new HashMap<>(), sessions=new HashMap<>(), prescriptions=new HashMap<>();
            for (var programme:source) {
                long oldProgramme=id(programme);
                programme.put("owner_reference",owner); shiftDates(programme,days);
                long newProgramme=insert("training_programs",programme); programmes.put(oldProgramme,newProgramme);
                for (var version:rows("select * from training_program_versions where program_id=? order by revision",oldProgramme)) {
                    long oldVersion=id(version); version.put("program_id",newProgramme); shiftDates(version,days);
                    long newVersion=insert("training_program_versions",version); versions.put(oldVersion,newVersion);
                    for (var session:rows("select * from training_program_sessions where version_id=? order by position",oldVersion)) {
                        long oldSession=id(session); session.put("version_id",newVersion);
                        long newSession=insert("training_program_sessions",session); sessions.put(oldSession,newSession);
                        for (var prescription:rows("select * from training_prescriptions where session_id=? order by position",oldSession)) {
                            long oldPrescription=id(prescription); prescription.put("session_id",newSession);
                            prescriptions.put(oldPrescription,insert("training_prescriptions",prescription));
                        }
                    }
                }
            }
            for (var workout:history) {
                long oldWorkout=id(workout);
                workout.put("owner_reference",owner); workout.put("active_owner",null);
                workout.put("program_id",mapped(programmes,workout.get("program_id")));
                workout.put("program_version_id",mapped(versions,workout.get("program_version_id")));
                workout.put("source_session_id",mapped(sessions,workout.get("source_session_id")));
                shiftDates(workout,days);
                Instant end=instant(workout.get("ended_at"));
                if (end.isAfter(clock.instant())) {
                    Duration advance=Duration.between(clock.instant().minusSeconds(60),end);
                    workout.put("started_at",instant(workout.get("started_at")).minus(advance).atOffset(ZoneOffset.UTC));
                    workout.put("ended_at",end.minus(advance).atOffset(ZoneOffset.UTC));
                }
                long newWorkout=insert("training_workouts",workout);
                for (var exercise:rows("select * from training_workout_exercises where workout_id=? order by position",oldWorkout)) {
                    long oldExercise=id(exercise); exercise.put("workout_id",newWorkout);
                    exercise.put("source_prescription_id",mapped(prescriptions,exercise.get("source_prescription_id")));
                    long newExercise=insert("training_workout_exercises",exercise);
                    for (var set:rows("select * from training_workout_sets where exercise_id=? order by position",oldExercise)) {
                        set.put("exercise_id",newExercise); insert("training_workout_sets",set);
                    }
                }
            }
            for (var schedule:rows("select * from training_schedules where owner_reference=? and workout_id is null and cancelled=false order by scheduled_at",TEMPLATE_OWNER)) {
                if (!programmes.containsKey(((Number)schedule.get("program_id")).longValue())) continue;
                schedule.put("owner_reference",owner); schedule.put("version",0);
                schedule.put("program_id",mapped(programmes,schedule.get("program_id")));
                schedule.put("session_id",mapped(sessions,schedule.get("session_id")));
                Instant date=instant(schedule.get("scheduled_at")).plus(days,ChronoUnit.DAYS);
                // Preserve existing personal slots and never change an existing schedule.
                if (!date.isAfter(clock.instant())) date=LocalDate.now(clock.withZone(ZONE)).plusDays(1).atTime(18,30).atZone(ZONE).toInstant();
                while (sql.queryForObject("select count(*) from training_schedules where owner_reference=? and occupied_at=?",Integer.class,owner,date.atOffset(ZoneOffset.UTC))>0) date=date.plusSeconds(60);
                schedule.put("scheduled_at",date.atOffset(ZoneOffset.UTC)); schedule.put("occupied_at",date.atOffset(ZoneOffset.UTC));
                insert("training_schedules",schedule);
            }
            // Marker and every inserted record commit together; any error rolls the entire copy back.
            sql.update("insert into training_demo_seeds(id) values(?)",marker);
        });
    }

    private Set<String> templateNames() {
        try (var input=new ClassPathResource("training/demo/programmes.json").getInputStream()) {
            Set<String> names=new HashSet<>();
            for (var programme:new ObjectMapper().readTree(input)) names.add(programme.get("name").asText());
            return names;
        } catch (java.io.IOException failure) {throw new IllegalStateException("Portable Training fixture missing",failure);}
    }
    private boolean marked(String marker) {
        return sql.queryForObject("select count(*) from training_demo_seeds where id=?",Integer.class,marker)>0;
    }
    private List<Map<String,Object>> rows(String query,Object... args) {
        return sql.query(query,(rs,n) -> {
            Map<String,Object> row=new LinkedHashMap<>(); var metadata=rs.getMetaData();
            for (int i=1;i<=metadata.getColumnCount();i++) row.put(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT),rs.getObject(i));
            return row;
        },args);
    }
    private long id(Map<String,Object> row) { return ((Number)row.get("id")).longValue(); }
    private long mapped(Map<Long,Long> ids,Object old) {
        Long value=ids.get(((Number)old).longValue());
        if (value==null) throw new IllegalStateException("Inconsistent local Training fixture relationship");
        return value;
    }
    private Instant instant(Object value) {
        if (value instanceof OffsetDateTime date) return date.toInstant();
        if (value instanceof Timestamp date) return date.toInstant();
        if (value instanceof Instant date) return date;
        throw new IllegalStateException("Unsupported local fixture timestamp");
    }
    private void shiftDates(Map<String,Object> row,long days) {
        for (String column:List.of("created_at","updated_at","started_at","ended_at")) {
            Object value=row.get(column);
            if (value!=null) row.put(column,instant(value).plus(days,ChronoUnit.DAYS).atOffset(ZoneOffset.UTC));
        }
    }
    private long insert(String table,Map<String,Object> original) {
        Map<String,Object> row=new LinkedHashMap<>(original); row.remove("id");
        String query="insert into "+table+" ("+String.join(",",row.keySet())+") values ("+String.join(",",Collections.nCopies(row.size(),"?"))+")";
        var key=new GeneratedKeyHolder();
        sql.update(connection -> {
            var statement=connection.prepareStatement(query,new String[]{"ID"});
            int i=1; for (Object value:row.values()) statement.setObject(i++,value);
            return statement;
        },key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
}
