package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.AuthResponse;
import ar.edu.um.ingenieria.limitador.dto.LoginRequest;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final String HASHED_PASSWORD =
            "$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private User user() {
        var user = new User();
        user.setId(1L);
        user.setUsername("jperez");
        user.setEmail("jperez@um.edu.ar");
        user.setPassword(HASHED_PASSWORD);
        user.setActive(true);
        user.getRoles().add(new Role("USER"));
        return user;
    }

    private LoginRequest request(String username, String password) {
        return new LoginRequest(username, password);
    }

    @Test
    void login_withValidCredentials_returnsTokenAndPublicDataWithoutPassword() {
        User user = user();
        Authentication successful = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(successful);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("generated-jwt");

        AuthResponse detail = authenticationService.login(request("jperez", "Secreto123"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        var token = (UsernamePasswordAuthenticationToken) captor.getValue();
        assertThat(token.getPrincipal()).isEqualTo("jperez");
        assertThat(token.getCredentials()).isEqualTo("Secreto123");

        assertThat(detail.token()).isEqualTo("generated-jwt");
        assertThat(detail.username()).isEqualTo("jperez");
        assertThat(detail.email()).isEqualTo("jperez@um.edu.ar");
        assertThat(detail.active()).isTrue();
        assertThat(detail.roles()).containsExactly("USER");
        assertThat(detail.toString()).doesNotContain("Secreto123").doesNotContain(HASHED_PASSWORD);
    }

    @Test
    void login_withWrongPassword_propagatesBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request("jperez", "mala")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withUnknownUser_propagatesBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request("noexiste", "Secreto123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withInactiveUser_propagatesDisabled() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authenticationService.login(request("jperez", "Secreto123")))
                .isInstanceOf(DisabledException.class);
    }
}
