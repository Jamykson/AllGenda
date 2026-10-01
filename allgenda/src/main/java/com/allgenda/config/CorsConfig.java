package com.allgenda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String codespaceName = System.getenv("CODESPACE_NAME");
        String forwardingDomain = System.getenv("GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN");
        List<String> originPatterns = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));

        if (codespaceName != null && forwardingDomain != null) {
            originPatterns.add("https://" + codespaceName + "-*" + "." + forwardingDomain);
        }

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOriginPatterns(originPatterns.toArray(String[]::new))
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                    .allowedHeaders("*");
            }
        };
    }
}