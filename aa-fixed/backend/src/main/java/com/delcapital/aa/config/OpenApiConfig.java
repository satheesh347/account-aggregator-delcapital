package com.delcapital.aa.config;

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
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Del Capital — Account Aggregator API")
                .description("""
                    API for consent-driven financial data retrieval via the Digio AA framework.
                    
                    **Flow:**
                    1. `POST /v1/consents` — create consent, redirect customer to `consentUrl`
                    2. Customer signs → Digio calls `/v1/webhook/digio` with status update
                    3. `POST /v1/fi/fetch` — initiate data fetch once consent is ACTIVE
                    4. `GET /v1/fi/fetch/{sessionId}/accounts` — retrieve normalized FI data
                    """)
                .version("1.0.0")
                .contact(new Contact().name("Del Capital Tech").email("tech.career@del-capital.com"))
                .license(new License().name("Proprietary")))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .name("BearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
