package com.polarbookshop.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchange ->
                        exchange.anyExchange().authenticated()
                )
                .oauth2ResourceServer(
                        ServerHttpSecurity.OAuth2ResourceServerSpec::jwt
                )
                .requestCache(requestCacheSpec -> // 액세스 토큰을 가지기 때문에 세션 캐시를 유지할 필요가 없다
                        requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
