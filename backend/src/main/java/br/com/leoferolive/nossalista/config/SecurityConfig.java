package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.OAuth2SuccessHandler;
import br.com.leoferolive.nossalista.auth.service.SessionCookieService;
import br.com.leoferolive.nossalista.mcpoauth.security.McpOAuthTokenAuthenticationFilter;
import br.com.leoferolive.nossalista.mcpoauth.security.McpWwwAuthenticateEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuração de segurança da aplicação NossaLista.
 * Define políticas de CORS, sessão web por cookie HttpOnly, OAuth2 e endpoints públicos.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String PERMISSIONS_POLICY =
        "accelerometer=(), autoplay=(), camera=(), display-capture=(), encrypted-media=(), "
            + "fullscreen=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), "
            + "payment=(), usb=()";
    private static final String CONTENT_SECURITY_POLICY =
        "base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'";

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PersonalAccessTokenAuthenticationFilter personalAccessTokenAuthenticationFilter;
    private final McpOAuthTokenAuthenticationFilter mcpOAuthTokenAuthenticationFilter;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final Http401UnauthorizedEntryPoint unauthorizedEntryPoint;
    private final Http403AccessDeniedHandler accessDeniedHandler;
    private final McpWwwAuthenticateEntryPoint mcpWwwAuthenticateEntryPoint;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final SessionCookieService sessionCookieService;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        PersonalAccessTokenAuthenticationFilter personalAccessTokenAuthenticationFilter,
        McpOAuthTokenAuthenticationFilter mcpOAuthTokenAuthenticationFilter,
        OAuth2SuccessHandler oauth2SuccessHandler,
        Http401UnauthorizedEntryPoint unauthorizedEntryPoint,
        Http403AccessDeniedHandler accessDeniedHandler,
        McpWwwAuthenticateEntryPoint mcpWwwAuthenticateEntryPoint,
        CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
        SessionCookieService sessionCookieService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.personalAccessTokenAuthenticationFilter = personalAccessTokenAuthenticationFilter;
        this.mcpOAuthTokenAuthenticationFilter = mcpOAuthTokenAuthenticationFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.mcpWwwAuthenticateEntryPoint = mcpWwwAuthenticateEntryPoint;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.sessionCookieService = sessionCookieService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        configureHeaders(http);
        configureCsrf(http);
        configureCors(http);
        configureAuthorization(http);
        configureStatelessSessions(http);
        configureExceptionHandling(http);
        configureOAuth2Login(http);
        configureAuthenticationFilters(http);
        return http.build();
    }

    private void configureHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
            .contentTypeOptions(contentType -> { })
            .frameOptions(frame -> frame.deny())
            .permissionsPolicyHeader(policy -> policy.policy(PERMISSIONS_POLICY))
            .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
            .addHeaderWriter(this::preventCachingOfAuthResponses));
    }

    private void preventCachingOfAuthResponses(HttpServletRequest request, HttpServletResponse response) {
        if (isAuthOrOAuthPath(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }
    }

    /**
     * Sessões web usam cookie HttpOnly; mutações autenticadas exigem o token XSRF legível pela SPA.
     * PAT/MCP continuam sem CSRF.
     */
    private void configureCsrf(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            .requireCsrfProtectionMatcher(csrfProtectionMatcher()));
    }

    private void configureCors(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
    }

    private void configureAuthorization(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> {
            configureSessionOnlyPaths(auth);
            configurePublicPaths(auth);
            configureMcpPaths(auth);
            configureApiPaths(auth);
        });
    }

    /**
     * Gestão de PATs e conexões OAuth exige sessão web por cookie: um PAT ou access token OAuth MCP
     * nunca cria, lista, revoga, aprova consentimento, nem gerencia conexões em nome do usuário.
     */
    private void configureSessionOnlyPaths(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
    ) {
        auth.requestMatchers("/api/users/me/tokens/**").access(sessionOnlyManager());
        auth.requestMatchers("/api/oauth/consent/**", "/api/oauth/connections/**").access(sessionOnlyManager());
    }

    /**
     * Endpoints públicos não requerem autenticação, mas um PAT nunca pode ser usado neles (por
     * exemplo, login e registro), mesmo que a rota seja pública.
     *
     * <p>O servidor OAuth 2.1 do MCP (D-022) e Dynamic Client Registration (D-024) são públicos por
     * natureza do protocolo; {@code /oauth/register} mantém rate limit por IP e validação de URIs.</p>
     */
    private void configurePublicPaths(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
    ) {
        auth.requestMatchers("/api/auth/**", "/api/health", "/actuator/health", "/actuator/prometheus", "/actuator/info")
            .access(publicUnlessPatManager());
        auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
        auth.requestMatchers("/oauth/authorize", "/oauth/token", "/oauth/revoke", "/oauth/register").permitAll();
        auth.requestMatchers("/.well-known/oauth-authorization-server", "/.well-known/oauth-protected-resource").permitAll();
        auth.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/lists/join/**").permitAll();
        auth.requestMatchers("/ws/**").permitAll();
    }

    /**
     * MCP Streamable HTTP aceita PAT, access token OAuth MCP ou sessão cookie válida. Não usa
     * {@code apiAccessManager}: todo o protocolo é POST, e o enforcement de escopo é por tool em
     * {@code McpSecurityContext}.
     */
    private void configureMcpPaths(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
    ) {
        auth.requestMatchers("/mcp/**").authenticated();
    }

    /** API PATs with READ scope may use only GET, HEAD, and OPTIONS. */
    private void configureApiPaths(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
    ) {
        auth.requestMatchers("/api/**").access(apiAccessManager());
        auth.anyRequest().permitAll();
    }

    private void configureStatelessSessions(HttpSecurity http) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    /**
     * APIs REST retornam RFC 7807 (401/403, sem redirect). MCP anuncia OAuth discovery (RFC 9728)
     * antes do 401 padrão. {@link DelegatingAuthenticationEntryPoint} é necessário porque
     * {@code defaultAuthenticationEntryPointFor} é ignorado quando há entry point explícito.
     */
    private void configureExceptionHandling(HttpSecurity http) throws Exception {
        http.exceptionHandling(exception -> exception
            .authenticationEntryPoint(mcpAuthenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler));
    }

    private DelegatingAuthenticationEntryPoint mcpAuthenticationEntryPoint() {
        return new DelegatingAuthenticationEntryPoint(unauthorizedEntryPoint, new RequestMatcherEntry<>(
            PathPatternRequestMatcher.withDefaults().matcher("/mcp/**"), mcpWwwAuthenticateEntryPoint));
    }

    /**
     * O authorization request (state anti-CSRF) é guardado em cookie, não em {@code HttpSession},
     * pois a aplicação é stateless. Sem isso o callback do Google não persiste state e pode emitir
     * múltiplos one-time codes órfãos.
     */
    private void configureOAuth2Login(HttpSecurity http) throws Exception {
        http.oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(authorization -> authorization.authorizationRequestRepository(authorizationRequestRepository))
            .redirectionEndpoint(redirect -> redirect.baseUri("/api/auth/google/callback"))
            .successHandler(oauth2SuccessHandler));
    }

    /**
     * Registra JWT antes da âncora UsernamePasswordAuthenticationFilter; PAT e OAuth MCP rodam antes
     * dele. O Spring Security só aceita JwtAuthenticationFilter como âncora após registrar sua posição.
     */
    private void configureAuthenticationFilters(HttpSecurity http) {
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(personalAccessTokenAuthenticationFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(mcpOAuthTokenAuthenticationFilter, JwtAuthenticationFilter.class);
    }

    private boolean isAuthOrOAuthPath(String path) {
        return path.startsWith("/api/auth/")
            || path.startsWith("/oauth2/")
            || path.startsWith("/login/oauth2/")
            || path.startsWith("/oauth/")
            || path.startsWith("/.well-known/oauth-");
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
            .path("/")
            .secure(sessionCookieService.isSecure()));
        return repository;
    }

    private RequestMatcher csrfProtectionMatcher() {
        RequestMatcher externalTransport = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/ws/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/mcp/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/oauth/token"),
            PathPatternRequestMatcher.withDefaults().matcher("/oauth/revoke"),
            PathPatternRequestMatcher.withDefaults().matcher("/oauth/register")
        );

        return request -> CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)
            && sessionCookieService.hasSessionCookie(request)
            && !externalTransport.matches(request);
    }

    /**
     * Permite a requisição a menos que tenha sido autenticada via PAT.
     * Usado nos endpoints públicos ({@code /api/auth/**}, health checks):
     * anônimos e sessões cookie passam normalmente; PATs são bloqueados (403).
     */
    private AuthorizationManager<RequestAuthorizationContext> publicUnlessPatManager() {
        return (authentication, context) ->
            new AuthorizationDecision(!PatAuthorizationSupport.isPersonalAccessToken(authentication.get()));
    }

    /**
     * Exige sessão JWT autenticada e bloqueia PATs. Usado na gestão de tokens
     * ({@code /api/users/me/tokens/**}): um PAT nunca pode gerenciar tokens.
     */
    private AuthorizationManager<RequestAuthorizationContext> sessionOnlyManager() {
        return (authentication, context) -> {
            var auth = authentication.get();
            boolean granted = PatAuthorizationSupport.isAuthenticatedUser(auth)
                && !PatAuthorizationSupport.isPersonalAccessToken(auth);
            return new AuthorizationDecision(granted);
        };
    }

    /**
     * Regra geral de {@code /api/**}: exige autenticação e, quando a
     * autenticação é um PAT de escopo READ, restringe a métodos HTTP seguros
     * (GET/HEAD/OPTIONS). PATs de escopo READ_WRITE e sessões cookie não sofrem
     * essa restrição adicional.
     *
     * <p>Um access token OAuth do servidor MCP (Fase C, D-022) é sempre negado
     * aqui, mesmo em métodos seguros — diferente de um PAT, seu escopo só vale
     * para {@code /mcp}, nunca para a API REST do SPA (defesa em profundidade:
     * o filtro que autentica esse token já só atua em {@code /mcp/**}, mas essa
     * checagem garante o bloqueio mesmo que isso mude).</p>
     */
    private AuthorizationManager<RequestAuthorizationContext> apiAccessManager() {
        return (authentication, context) -> {
            var auth = authentication.get();
            if (!PatAuthorizationSupport.isAuthenticatedUser(auth)) {
                return new AuthorizationDecision(false);
            }
            if (PatAuthorizationSupport.isMcpOAuthToken(auth)) {
                return new AuthorizationDecision(false);
            }
            if (PatAuthorizationSupport.isPersonalAccessToken(auth) && PatAuthorizationSupport.hasReadOnlyScope(auth)) {
                boolean safeMethod = PatAuthorizationSupport.isSafeMethod(context.getRequest().getMethod());
                return new AuthorizationDecision(safeMethod);
            }
            return new AuthorizationDecision(true);
        };
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
