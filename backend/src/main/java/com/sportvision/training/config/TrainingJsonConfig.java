package com.sportvision.training.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Keep training input strict without changing the other module's JSON contract. */
@Configuration
public class TrainingJsonConfig implements WebMvcConfigurer {
    private final ObjectMapper mapper;
    public TrainingJsonConfig(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper strict = mapper.copy()
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        converters.add(0, new MappingJackson2HttpMessageConverter(strict) {
            private boolean training(Class<?> type) {
                return type.getPackageName().startsWith("com.sportvision.training.dto");
            }
            @Override
            public boolean canRead(Class<?> type, MediaType mediaType) {
                return training(type) && super.canRead(type, mediaType);
            }
            @Override
            public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
                return training(getJavaType(type, contextClass).getRawClass())
                    && super.canRead(type, contextClass, mediaType);
            }
            @Override
            public boolean canWrite(Class<?> type, MediaType mediaType) { return false; }
            @Override
            public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) { return false; }
        });
    }
}
