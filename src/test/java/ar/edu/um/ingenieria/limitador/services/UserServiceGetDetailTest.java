package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserDetail;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceGetDetailTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDataRepository userDataRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getDetail_withExistingUser_returnsDetail() {
        // Arrange
        var user = new User();
        user.setId(1L);
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setActive(true);
        user.getRoles().add(new Role("ADMIN"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserDetail detail = userService.getDetail(1L);

        // Assert
        assertThat(detail.id()).isEqualTo(1L);
        assertThat(detail.username()).isEqualTo("jdoe");
        assertThat(detail.email()).isEqualTo("jdoe@example.com");
        assertThat(detail.active()).isTrue();
        assertThat(detail.roles()).containsExactly("ADMIN");
    }

    @Test
    void getDetail_withUnknownId_throwsNotFound() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.getDetail(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getDetail_withNullId_throwsBadRequestWithoutHittingRepository() {
        // Act + Assert
        assertThatThrownBy(() -> userService.getDetail(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    void getDetail_withNonPositiveId_throwsBadRequestWithoutHittingRepository(long id) {
        // Act + Assert
        assertThatThrownBy(() -> userService.getDetail(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(userRepository);
    }

    @Test
    void getDetail_withCorruptedUser_propagatesIllegalState() {
        // Arrange
        var user = new User();
        user.setId(2L);
        user.setUsername(" ");
        user.setEmail("x@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        // Act + Assert
        assertThatThrownBy(() -> userService.getDetail(2L))
                .isInstanceOf(IllegalStateException.class);
    }
}
