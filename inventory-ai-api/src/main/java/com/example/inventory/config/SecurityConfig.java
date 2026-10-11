package com.example.inventory.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
//turn on permission check
// Analogy: the filter chain is the front door of a building.
// Method security is the lock on each individual office door inside.
// @EnableMethodSecurity is what installs those inner locks. Without it,
// you can write “ADMIN ONLY” on an office door, but nothing enforces it.
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthenticationConverter converter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/**", "/v3/api-docs/**"
                                , "/swagger-ui.html", "/swagger-ui/**",
                                "/products/**", "/api/auth/**")
                        .permitAll()
                        .anyRequest().authenticated())
//                the guard. Read "Authorization: Bearer <token>"
                .oauth2ResourceServer(o-> o.jwt(j->
                        j.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key){
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(){
        var authorities = new JwtGrantedAuthoritiesConverter();
        // setting: "look in the claim named roles"
        // configure: read authorities from the "roles" claim
        authorities.setAuthoritiesClaimName("roles");
        // setting: "add ROLE_ in front"
        // configure: prefix each one with ROLE_
        authorities.setAuthorityPrefix("ROLE_");
        // handle role to authorities step
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }


}
