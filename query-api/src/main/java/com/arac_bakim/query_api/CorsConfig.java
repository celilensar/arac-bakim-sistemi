package com.arac_bakim.query_api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS: React gelistirme sunucusu (ornegin http://localhost:5173) ayri bir
 * origin oldugundan, tarayici guvenlik geregi API cagrisini engeller.
 * Burada /api/** uclarina o origin'den GET izni veriyoruz.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET");
    }
}
