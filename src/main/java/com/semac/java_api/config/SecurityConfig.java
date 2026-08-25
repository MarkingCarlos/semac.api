package com.semac.java_api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/* Autenticação por Bearer token (JWT HS256). O login gera o token; cada
   requisição protegida o envia em Authorization: Bearer <token>. O
   resource-server decodifica, lê a claim `role` e a transforma em uma
   autoridade ROLE_<role> usada nas regras abaixo.

   Acesso ao módulo financeiro (apenas DIRETOR_SITE e PRESIDENTE):
   endpoints exclusivos do financeiro são trancados; os GET de
   patrocinador/cota/doador seguem abertos por serem usados pelo site
   público e pela /admin (ainda sem auth). */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PAPEIS_FINANCEIRO = { "DIRETOR_SITE", "PRESIDENTE" };

    private static final String PAPEL_PARTICIPANTE = "PARTICIPANTE";

    private final SecretKey chaveJwt;
    private final List<String> origensCors;

    public SecurityConfig(@Value("${jwt.secret}") String segredo,
                          @Value("${app.cors.origins:http://localhost:5173}") List<String> origensCors) {
        this.chaveJwt = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.origensCors = origensCors;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Públicos / abertos
                        .requestMatchers("/api/auth/login", "/api/inscricao").permitAll()
                        // Perfil próprio: qualquer usuário autenticado (identificado pelo token)
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/pessoa/me").authenticated()
                        // Agenda e escolha de minicurso: só o próprio participante confirmado
                        .requestMatchers(HttpMethod.GET, "/api/evento/meus").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.POST, "/api/evento/*/inscricao").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/evento/*/inscricao").hasRole(PAPEL_PARTICIPANTE)
                        // Exclusivos do financeiro
                        .requestMatchers("/api/compra/**", "/api/fornecedor/**", "/api/cotacao/**", "/api/conjunto/**", "/api/variacao/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers("/api/caixa-fundunesp/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/inscricoes").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Escrita de patrocínio/cota acontece só no financeiro (GET segue aberto)
                        .requestMatchers(HttpMethod.POST, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PUT, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PATCH, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Preço da camiseta avulsa — editado em Informações SEMAC. O GET
                        // segue aberto: o cadastro público precisa do preço.
                        .requestMatchers(HttpMethod.PUT, "/api/camiseta-extra").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Escrita de níveis de participante — gerenciada em Informações SEMAC (GET segue aberto)
                        .requestMatchers(HttpMethod.POST, "/api/nivel/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PUT, "/api/nivel/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/nivel/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Demais (site público + /admin, ainda sem auth) seguem abertos
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorAutenticacao())));
        return http.build();
    }

    /* Lê a claim `role` do token e a expõe como autoridade ROLE_<role>,
       que casa com hasAnyRole(...). */
    private JwtAuthenticationConverter conversorAutenticacao() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthorityPrefix("ROLE_");
        autoridades.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensCors);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
