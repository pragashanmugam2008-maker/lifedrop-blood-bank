package com.lifedrop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lifeDropOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("LifeDrop - Blood Bank Management System API")
                        .description("Production-quality RESTful API documentation for the LifeDrop Blood Bank Platform. " +
                                     "Supports Donor Management, Universal ABO/Rh Compatibility Engine, Live Database-Calculated Statistics, " +
                                     "Blood Inventory Tracking, Patient Blood Requests, Verified Donations, and Referral Star Rewards.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LifeDrop Engineering Team")
                                .email("support@lifedrop.org")
                                .url("https://lifedrop.org"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (without 'Bearer ' prefix) to authorize protected requests.")));
    }
}
