package com.sportvision.training.service;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import com.sportvision.training.entity.TrainingExercise;
import com.sportvision.training.entity.ExerciseOptions.*;
import com.sportvision.training.dto.*;
import com.sportvision.training.repository.TrainingExerciseRepository;

@Service
@Transactional(readOnly = true)
public class TrainingExerciseService {
    private final TrainingExerciseRepository repository;
    public TrainingExerciseService(TrainingExerciseRepository repository) { this.repository = repository; }

    public PageOutput<ExerciseOutput> list(String search, MuscleGroup muscleGroup, Equipment equipment,
        Difficulty difficulty, MeasurementType measurementType, ArchiveStatus status, int page, int size, String sort, String direction) {
        if (page < 0 || page > 100000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Pagination invalide : page de 0 à 100000, taille de 1 à 50.");
        if (!Set.of("name", "createdAt", "updatedAt", "difficulty", "id").contains(sort)
            || !Set.of("asc", "desc").contains(direction)) throw new IllegalArgumentException("Tri invalide.");
        if (search != null && search.length() > 120) throw new IllegalArgumentException("La recherche est limitée à 120 caractères.");
        Specification<TrainingExercise> spec = (root, query, cb) -> cb.conjunction();
        if (status != ArchiveStatus.ALL) spec = spec.and((r, q, cb) -> cb.equal(r.get("archived"), status == ArchiveStatus.ARCHIVED));
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("name")), pattern, '\\'));
        }
        if (muscleGroup != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("muscleGroup"), muscleGroup));
        if (equipment != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("equipment"), equipment));
        if (difficulty != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("difficulty"), difficulty));
        if (measurementType != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("measurementType"), measurementType));
        Sort order = Sort.by(Sort.Direction.fromString(direction), sort);
        if (!sort.equals("id")) order = order.and(Sort.by("id"));
        return PageOutput.from(repository.findAll(spec, PageRequest.of(page, size, order)).map(ExerciseOutput::from));
    }
    public ExerciseOutput get(long id) { return ExerciseOutput.from(find(id)); }
    private TrainingExercise find(long id) { return repository.findById(id).orElseThrow(ExerciseNotFoundException::new); }
    @Transactional
    public ExerciseOutput create(ExerciseInput input) { return save(new TrainingExercise(), input); }
    @Transactional
    public ExerciseOutput update(long id, ExerciseInput input) { return save(find(id), input); }
    private ExerciseOutput save(TrainingExercise exercise, ExerciseInput input) {
        exercise.edit(input.name(), input.description(), input.instructions(), input.muscleGroup(), input.equipment(), input.difficulty(), input.measurementType());
        return ExerciseOutput.from(repository.saveAndFlush(exercise));
    }
    @Transactional
    public ExerciseOutput archive(long id, boolean archived) {
        TrainingExercise exercise = find(id);
        exercise.setArchived(archived);
        return ExerciseOutput.from(repository.saveAndFlush(exercise));
    }
}
