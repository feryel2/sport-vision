package com.sportvision.training.service;
public class ProgramException extends RuntimeException {
    private final int status;
    public ProgramException(int status,String message){super(message);this.status=status;}
    public int getStatus(){return status;}
}
