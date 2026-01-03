package com.ecommerce.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Value("${spring.application.name:user-service}")
    private String applicationName;

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .components(securityComponents())
                .addSecurityItem(securityRequirement());
    }

    private Info apiInfo() {
        return new Info()
                .title("E-commerce User Service API")
                .description("""
                    Comprehensive User Service for e-commerce microservices architecture.
                """);
    }

    private List<Server> serverList() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort + "/api/v1")
                .description("Local Development Server");


        return List.of(localServer);
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("cookieAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("access_token")
                        .description("JWT access token stored in HttpOnly cookie. " +
                                "Automatically sent by browser on each request. "))
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Alternative: JWT Bearer token in Authorization header. " +
                                "Format: Authorization: Bearer token"));
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement()
                .addList("cookieAuth")
                .addList("bearerAuth");
    }
}