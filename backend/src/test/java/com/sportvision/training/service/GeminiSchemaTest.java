package com.sportvision.training.service;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GeminiSchemaTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final GeminiClient client=new GeminiClient(mapper,"unit-test-secret-not-real","gemini-2.5-flash",45);

    @Test void serializesOneSimpleSchemaAsObjectInTheActualRequestBody()throws Exception{
        JsonNode schema=mapper.readTree(getClass().getResourceAsStream("/training/ai/program-schema.json"));
        JsonNode body=mapper.readTree(client.requestBody("Consignes de test","{}",schema));
        JsonNode config=body.path("generationConfig");
        assertThat(config.path("responseJsonSchema").isObject()).isTrue();
        assertThat(config.path("responseJsonSchema")).isEqualTo(schema);
        assertThat(config.has("responseSchema")||config.has("responseFormat")||config.has("_responseJsonSchema")).isFalse();
        assertThat(config.path("responseMimeType").asText()).isEqualTo("application/json");
        checkSimpleSchema(schema);
        Files.writeString(Path.of("target/gemini-generation-config.checked.json"),config.toPrettyString());
    }
    private void checkSimpleSchema(JsonNode schema){
        schema.fieldNames().forEachRemaining(key->assertThat(Set.of("type","properties","required","items")).contains(key));
        assertThat(schema.path("type").isTextual()).isTrue();
        schema.path("properties").forEach(this::checkSimpleSchema);
        if(schema.has("items"))checkSimpleSchema(schema.get("items"));
    }
    @Test void preservesTechnicalMessageAndFieldPathWhileRedactingSecrets()throws Exception{
        String path="generation_config.response_json_schema.properties.sessions.items";
        String message="Schema has too many states. unit-test-secret-not-real https://example.invalid/?key=hidden person@example.invalid";
        var error=mapper.createObjectNode();
        var detail=error.putObject("error").put("status","INVALID_ARGUMENT").put("message",message);
        detail.putArray("details").addObject().putArray("fieldViolations").addObject()
            .put("field",path).put("description","Unsupported constraint at "+path);
        String result=client.providerDiagnostic(400,error.toString(),"{}");
        JsonNode diagnostic=mapper.readTree(result);
        assertThat(diagnostic.path("message").asText()).startsWith("Schema has too many states.");
        assertThat(diagnostic.at("/fieldViolations/0/field").asText()).isEqualTo(path);
        assertThat(result).doesNotContain("unit-test-secret-not-real","https://","person@example.invalid");
    }
}
