package com.sportvision.training.dto;

import java.util.Arrays;
import java.util.List;
import com.sportvision.training.entity.ExerciseOptions.*;

public record OptionsOutput(List<Option> muscleGroups, List<Option> equipment, List<Option> difficulties, List<Option> measurementTypes) {
    public record Option(String value, String label) {}
    private static <E extends Enum<E> & Labeled> List<Option> options(E[] values) {
        return Arrays.stream(values).map(v -> new Option(v.name(), v.label())).toList();
    }
    public static OptionsOutput all() {
        return new OptionsOutput(options(MuscleGroup.values()), options(Equipment.values()), options(Difficulty.values()), options(MeasurementType.values()));
    }
}
