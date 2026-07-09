package com.duoc.ms_administracion_archivos.config;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para ms-administracion-archivos.
 *
 * Azure AD B2C entrega un custom claim "extension_consultaRole" (mismo patrón
 * usado en plataforma-educativa, Semanas 4-5), pero aquí se usan DOS valores
 * posibles según el requerimiento del caso:
 *
 *   - "descarga"  -> solo puede usar el endpoint de descargar guías
 *   - "gestion"   -> puede usar el resto de los endpoints (crear, subir,
 *                    modificar, eliminar, consultar)
 *
 * IMPORTANTE: el nombre del claim y de los roles debe coincidir EXACTO con lo
 * que configures en el User Flow de Azure AD B2C (Paso 2 y 4 de la guía).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize en el controller
public class SecurityConfig {

    
    // (Azure AD B2C > User Flow > Run user flow > copiar enlace generado)
    private static final String JWK_SET_URI =
            "https://guiasdespacho2.b2clogin.com/guiasdespacho2.onmicrosoft.com/discovery/v2.0/keys?p=b2c_1_registro_login";

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.sameOrigin())
        )
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
        );
    return http.build();
}

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
    }

    /**
     * Traduce el custom claim "extension_consultaRole" de Azure AD B2C en un
     * GrantedAuthority de Spring Security con prefijo ROLE_, para poder usar
     * @PreAuthorize("hasRole('DESCARGA')") / @PreAuthorize("hasRole('GESTION')").
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new HashSet<>();
            String rol = jwt.getClaimAsString("extension_consultaRole");
            if (rol != null && !rol.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
            }
            return authorities;
        });
        return converter;
    }
}