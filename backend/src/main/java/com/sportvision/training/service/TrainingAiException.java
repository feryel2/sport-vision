package com.sportvision.training.service;
/** Erreurs publiques contrôlées : ne jamais y placer la réponse brute ou une clé. */
public class TrainingAiException extends RuntimeException {
    private final int status;private final String code;
    public TrainingAiException(int status,String code,String message){super(message);this.status=status;this.code=code;}
    public int getStatus(){return status;}public String getCode(){return code;}
}
