package com.c0nnect.nutripick.global.security;

import com.c0nnect.nutripick.global.error.SecurityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApiSecurityConfig {
    @Bean
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            SecurityExceptionHandler securityExceptionHandler) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler)
                )
                .build();
    }
}
