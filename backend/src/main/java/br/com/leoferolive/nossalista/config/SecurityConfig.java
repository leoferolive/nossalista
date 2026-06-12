package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final Http401UnauthorizedEntryPoint unauthorizedEntryPoint;
    private final Http403AccessDeniedHandler accessDeniedHandler;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        OAuth2SuccessHandler oauth2SuccessHandler,
        Http401UnauthorizedEntryPoint unauthorizedEntryPoint,
        Http403AccessDeniedHandler accessDeniedHandler,
        CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authorizationRequestRepository = authorizationRequestRepository;
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
                        .requestMatchers("/api/auth/**", "/api/health", "/actuator/health", "/actuator/prometheus").permitAll()
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

                // Exception Handling - RFC 7807 para APIs REST (não redirecionar):
                // 401 quando não autenticado, 403 quando autenticado sem authority
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Configurar OAuth2 Login.
                // O authorization-request (state anti-CSRF) é guardado em COOKIE e não
                // na HttpSession — obrigatório porque a app é STATELESS. Sem isso o
                // state não persiste, o callback do Google vira não-idempotente e
                // emite múltiplos one-time codes órfãos (login Google nunca completa).
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestRepository(authorizationRequestRepository)
                        )
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

        // Origens permitidas (configuradas por profile em application*.yml)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

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
