package com.sportvision.training.service;
public class WorkoutException extends RuntimeException {
    private final int status;private final String code;private final Long workoutId;private final int pendingSets;
    public WorkoutException(int status,String message,String code,Long workoutId,int pendingSets){super(message);this.status=status;this.code=code;this.workoutId=workoutId;this.pendingSets=pendingSets;}
    public int getStatus(){return status;} public String getCode(){return code;} public Long getWorkoutId(){return workoutId;} public int getPendingSets(){return pendingSets;}
}
