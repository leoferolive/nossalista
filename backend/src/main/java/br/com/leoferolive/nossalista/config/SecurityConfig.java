package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuração de segurança da aplicação NossaLista.
 * Define políticas de CORS, autenticação JWT, OAuth2 e endpoints públicos.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final Http401UnauthorizedEntryPoint unauthorizedEntryPoint;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        OAuth2SuccessHandler oauth2SuccessHandler,
        Http401UnauthorizedEntryPoint unauthorizedEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilitar CSRF - API stateless não usa cookies de sessão
                .csrf(csrf -> csrf.disable())

                // Configurar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configurar autorização de endpoints
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos - não requerem autenticação
                        .requestMatchers("/api/auth/**", "/api/health", "/actuator/health").permitAll()
                        // OAuth2 endpoints (Spring Security gerencia automaticamente)
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Endpoint de join via convite - GET é público (read-only), POST requer auth
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/lists/join/**").permitAll()
                        // WebSocket endpoint - auth feita pelo WebSocketAuthInterceptor
                        .requestMatchers("/ws/**").permitAll()
                        // Endpoints da API requerem autenticação
                        .requestMatchers("/api/**").authenticated()
                        // Rotas do SPA embutido (/, /listas, /perfil, assets, etc)
                        .anyRequest().permitAll()
                )

                // Session Management - Stateless (sem sessões server-side)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Exception Handling - Retornar 401 RFC 7807 para APIs REST (não redirecionar)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedEntryPoint)
                )

                // Configurar OAuth2 Login
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(redirect ->
                                redirect.baseUri("/api/auth/google/callback")
                        )
                        .successHandler(oauth2SuccessHandler)
                )

                // Adicionar JWT Authentication Filter ANTES de UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuração de CORS para permitir requisições do frontend.
     * Permite apenas origens confiáveis em produção.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origens permitidas
        configuration.setAllowedOrigins(Arrays.asList(
                "https://nossalista.leoferolive.com.br",  // Produção
                "http://nossalista.home",                  // Dev K8s
                "http://localhost:5173",                   // Desenvolvimento (Vite)
                "http://localhost:8080"                    // Desenvolvimento (container)
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Permitir credenciais (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expor headers de resposta (útil para paginação, etc)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        // Aplicar configuração a todos os endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
