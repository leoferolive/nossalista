package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import br.com.leoferolive.nossalista.auth.exception.InvalidOAuthCodeException;
import br.com.leoferolive.nossalista.auth.repository.OAuthAuthorizationCodeRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthExchangeService (Q2.3)")
class OAuthExchangeServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private OAuthAuthorizationCodeRepository codeRepository;

    private final Map<String, OAuthAuthorizationCode> issuedCodes = new HashMap<>();

    private OAuthCodeStore oauthCodeStore;
    private OAuthExchangeService exchangeService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // OAuthCodeStore agora é persistido. Repositório mockado com um mapa por
        // trás preserva o round-trip issue->consume deste teste unitário.
        lenient().when(codeRepository.save(any(OAuthAuthorizationCode.class))).thenAnswer(inv -> {
            OAuthAuthorizationCode entity = inv.getArgument(0);
            issuedCodes.put(entity.getCode(), entity);
            return entity;
        });
        lenient().when(codeRepository.findByCode(anyString()))
            .thenAnswer(inv -> Optional.ofNullable(issuedCodes.get(inv.<String>getArgument(0))));
        lenient().doAnswer(inv -> {
            OAuthAuthorizationCode entity = inv.getArgument(0);
            issuedCodes.remove(entity.getCode());
            return null;
        }).when(codeRepository).delete(any(OAuthAuthorizationCode.class));

        oauthCodeStore = new OAuthCodeStore(codeRepository);
        exchangeService = new OAuthExchangeService(oauthCodeStore, jwtService, userService);
    }

    private User buildUser() {
        User user = new User();
        user.setId(userId);
        user.setUsername("leo");
        user.setEmail("leo@gmail.com");
        user.setName("Leonardo");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        return user;
    }

    @Test
    @DisplayName("code válido → retorna sessão interna com o JWT")
    void validCodeReturnsLoginResponse() {
        String jwt = "valid.jwt.token";
        String code = oauthCodeStore.issue(jwt);

        when(jwtService.extractUserId(jwt)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(Optional.of(buildUser()));
        AuthenticatedSession session = exchangeService.exchange(code);

        assertThat(session.token()).isEqualTo(jwt);
        assertThat(session.user().getEmail()).isEqualTo("leo@gmail.com");
        assertThat(session.user().getUsername()).isEqualTo("leo");
    }

    @Test
    @DisplayName("code inválido/inexistente → lança InvalidOAuthCodeException")
    void invalidCodeThrows() {
        assertThatThrownBy(() -> exchangeService.exchange("inexistente"))
            .isInstanceOf(InvalidOAuthCodeException.class);
    }

    @Test
    @DisplayName("reuso do code → segunda troca lança InvalidOAuthCodeException")
    void reusedCodeThrows() {
        String jwt = "valid.jwt.token";
        String code = oauthCodeStore.issue(jwt);

        when(jwtService.extractUserId(jwt)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(Optional.of(buildUser()));
        // Primeira troca: sucesso
        exchangeService.exchange(code);

        // Segunda troca do mesmo code: falha (single-use)
        assertThatThrownBy(() -> exchangeService.exchange(code))
            .isInstanceOf(InvalidOAuthCodeException.class);
    }

    @Test
    @DisplayName("usuário do JWT não existe mais → lança InvalidOAuthCodeException")
    void missingUserThrows() {
        String jwt = "valid.jwt.token";
        String code = oauthCodeStore.issue(jwt);

        when(jwtService.extractUserId(jwt)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeService.exchange(code))
            .isInstanceOf(InvalidOAuthCodeException.class);
    }
}
