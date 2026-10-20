package com.sportvision.training.service;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class GeminiClient {
    private static final Logger LOG=LoggerFactory.getLogger(GeminiClient.class);
    private final ObjectMapper mapper;private final String apiKey,model;private final int timeoutSeconds;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    public GeminiClient(ObjectMapper mapper,@Value("${training.ai.api-key:}") String apiKey,
        @Value("${training.ai.model:gemini-2.5-flash}") String model,@Value("${training.ai.timeout-seconds:45}") int timeoutSeconds){
        this.mapper=mapper;this.apiKey=apiKey;this.model=model;this.timeoutSeconds=timeoutSeconds;
    }
    public void requireConfigured(){
        if(apiKey==null||apiKey.isBlank()||apiKey.equals("COLLEZ_VOTRE_CLE_ICI"))throw failure(503,"NOT_CONFIGURED","Service IA non configuré");
        if(!model.matches("[a-zA-Z0-9.-]{1,100}")||timeoutSeconds<5||timeoutSeconds>120)
            throw failure(503,"CONFIGURATION","Configuration IA invalide : vérifier le modèle et le délai (5 à 120 secondes).");
    }
    public String generate(String instructions,String data,JsonNode schema){
        requireConfigured();
        String body=requestBody(instructions,data,schema);
        var request=HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent"))
            .timeout(Duration.ofSeconds(timeoutSeconds)).header("Content-Type","application/json")
            .header("x-goog-api-key",apiKey).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        // Un seul appel. Le délai couvre aussi la réception complète du corps HTTP.
        var future=http.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        try{
            var response=future.get(timeoutSeconds,TimeUnit.SECONDS);
            checkStatus(response.statusCode(),response.body(),data);
            return extractText(response.body());
        }catch(TimeoutException ex){
            throw failure(504,"TIMEOUT","Délai IA dépassé. Réessayez plus tard ou créez le programme manuellement.");
        }catch(InterruptedException ex){
            Thread.currentThread().interrupt();throw failure(503,"UNAVAILABLE","Service IA indisponible.");
        }catch(ExecutionException ex){
            if(ex.getCause() instanceof HttpTimeoutException)throw failure(504,"TIMEOUT","Délai IA dépassé.");
            throw failure(503,"UNAVAILABLE","Service IA indisponible. Réessayez plus tard.");
        }finally{future.cancel(true);}
    }
    String requestBody(String instructions,String data,JsonNode schema){
        if(schema==null||!schema.isObject())throw failure(503,"CONFIGURATION","Le schéma IA doit être un objet JSON.");
        return json(Map.of("systemInstruction",Map.of("parts",List.of(Map.of("text",instructions))),
            "contents",List.of(Map.of("role","user","parts",List.of(Map.of("text",data)))),
            "generationConfig",Map.of("candidateCount",1,"maxOutputTokens",8192,
                "responseMimeType","application/json","responseJsonSchema",schema)));
    }
    private String json(Object value){
        try{return mapper.writeValueAsString(value);}catch(Exception ex){throw failure(503,"CONFIGURATION","Configuration IA indisponible.");}
    }
    private void checkStatus(int status,String body,String requestData){
        LOG.info("Gemini {}",providerDiagnostic(status,body,requestData));
        if(status>=200&&status<300)return;
        if(status==401||status==403||invalidKey(body))throw failure(503,"KEY_REJECTED","Clé IA refusée. Vérifiez la configuration côté serveur.");
        if(status==429)throw failure(429,"QUOTA","Quota IA atteint. Réessayez plus tard.");
        if(status==400||status==404)throw failure(503,"CONFIGURATION","Configuration IA refusée : vérifier le modèle et le format.");
        throw failure(503,"UNAVAILABLE","Service IA indisponible. Réessayez plus tard.");
    }
    String providerDiagnostic(int status,String body,String requestData){
        var diagnostic=mapper.createObjectNode().put("httpStatus",status).put("code","UNKNOWN");
        var violations=diagnostic.putArray("fieldViolations");
        if(status>=200&&status<300)return json(diagnostic.put("code","OK").put("message","Réponse reçue"));
        try{
            var error=mapper.readTree(body).path("error");
            String suppliedCode=error.path("status").asText();
            if(Set.of("INVALID_ARGUMENT","NOT_FOUND","PERMISSION_DENIED","UNAUTHENTICATED",
                "RESOURCE_EXHAUSTED","FAILED_PRECONDITION","INTERNAL","UNAVAILABLE","DEADLINE_EXCEEDED").contains(suppliedCode))diagnostic.put("code",suppliedCode);
            diagnostic.put("message",redactDiagnostic(error.path("message").asText("Message absent"),requestData));
            for(var detail:error.path("details"))for(var violation:detail.path("fieldViolations"))
                violations.addObject().put("field",redactDiagnostic(violation.path("field").asText(),requestData))
                    .put("description",redactDiagnostic(violation.path("description").asText(),requestData));
        }catch(Exception ignored){diagnostic.put("message","Erreur fournisseur illisible ; corps masqué");}
        return json(diagnostic);
    }
    private String redactDiagnostic(String text,String requestData){
        // Conserver le message technique exact, pas le corps HTTP ni les métadonnées fournisseur.
        if(apiKey!=null&&!apiKey.isBlank())text=text.replace(apiKey,"[SECRET MASQUÉ]");
        if(requestData!=null&&!requestData.isBlank())text=text.replace(requestData,"[DONNÉES MASQUÉES]");
        try{
            var data=mapper.readTree(requestData);
            for(var exercise:data.path("catalogue")){
                String name=exercise.path("name").asText();
                if(!name.isBlank())text=text.replace(name,"[LIBELLÉ MASQUÉ]");
            }
        }catch(Exception ignored){/* Aucune donnée de requête affichée. */}
        return text.replaceAll("https?://[^\\s<>\\\"]+","[URL MASQUÉE]")
            .replaceAll("(?i)(?:bearer\\s+|(?:x-goog-api-key|api[_-]?key|authorization)\\s*[:=]\\s*)[^\\s,;\\\"]+","[AUTH MASQUÉE]")
            .replaceAll("AIza[A-Za-z0-9_-]+","[SECRET MASQUÉ]")
            .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}","[EMAIL MASQUÉ]");
    }
    private boolean invalidKey(String body){
        try{for(var detail:mapper.readTree(body).path("error").path("details"))if("API_KEY_INVALID".equals(detail.path("reason").asText()))return true;}
        catch(Exception ignored){/* Ne jamais journaliser le corps d'erreur du fournisseur. */}
        return false;
    }
    private String extractText(String body){
        if(body==null||body.isBlank())throw failure(502,"EMPTY","Réponse IA vide.");
        if(body.length()>1_000_000)throw failure(502,"INVALID","Réponse IA trop volumineuse.");
        try{
            JsonNode root=mapper.readTree(body);
            if(root.path("promptFeedback").hasNonNull("blockReason"))throw failure(502,"BLOCKED","Proposition bloquée par le service IA.");
            var candidates=root.path("candidates");
            if(!candidates.isArray()||candidates.isEmpty())throw failure(502,"EMPTY","Réponse IA vide.");
            var candidate=candidates.get(0);String reason=candidate.path("finishReason").asText();
            if("MAX_TOKENS".equals(reason))throw failure(502,"TRUNCATED","Réponse IA tronquée. Aucun programme enregistré.");
            if(!"STOP".equals(reason))throw failure(502,"BLOCKED","Proposition IA bloquée ou interrompue.");
            StringBuilder text=new StringBuilder();
            for(var part:candidate.path("content").path("parts"))if(!part.path("thought").asBoolean(false)&&part.path("text").isTextual())text.append(part.get("text").asText());
            if(text.isEmpty())throw failure(502,"EMPTY","Réponse IA vide.");
            return text.toString();
        }catch(TrainingAiException ex){throw ex;}
        catch(Exception ex){throw failure(502,"INVALID","Réponse IA illisible. Aucun programme enregistré.");}
    }
    private TrainingAiException failure(int status,String code,String message){return new TrainingAiException(status,code,message);}
}
