package com.sportvision.training.entity;
public final class WorkoutStatuses {
    private WorkoutStatuses() {}
    public enum WorkoutStatus { IN_PROGRESS, COMPLETED, ABANDONED }
    public enum SetStatus { PENDING, DONE, SKIPPED }
}
