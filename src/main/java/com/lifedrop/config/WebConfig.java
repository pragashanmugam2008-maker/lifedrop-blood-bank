package com.lifedrop.config;

import com.lifedrop.enums.BloodGroup;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, BloodGroup>() {
            @Override
            public BloodGroup convert(@NonNull String source) {
                return BloodGroup.fromString(source);
            }
        });
    }
}
