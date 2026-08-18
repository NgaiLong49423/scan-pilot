package com.scanpilot.auth.config;

import com.scanpilot.auth.interceptor.AuthInterceptor;
import com.scanpilot.auth.resolver.AuthenticatedUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;
    private final AuthConfigProperties properties;

    public AuthWebMvcConfig(
            AuthInterceptor authInterceptor,
            AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver,
            AuthConfigProperties properties
    ) {
        this.authInterceptor = authInterceptor;
        this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.getFrontendUrl())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
