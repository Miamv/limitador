package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDataRepository userDataRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void save_withoutUserData_shouldPersistWithoutLookup() {
        User user = new User();
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setUserData(null);

        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.save(user);

        assertThat(result.getUserData()).isNull();
        verify(userDataRepository, never()).findById(any());
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void save_withExistingUserData_shouldResolveAndPersist() {
        UserData detached = new UserData();
        detached.setId(1L);

        UserData managed = new UserData();
        managed.setId(1L);
        managed.setFirstName("John");
        managed.setPhoneNumber("+5492615551234");

        User user = new User();
        user.setUsername("asmith");
        user.setEmail("asmith@example.com");
        user.setUserData(detached);

        when(userDataRepository.findById(1L)).thenReturn(Optional.of(managed));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.save(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDataRepository).findById(1L);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserData()).isSameAs(managed);
        assertThat(result.getUserData()).isSameAs(managed);
    }

    @Test
    void save_withNonExistingUserData_shouldThrowNotFoundAndNotPersist() {
        UserData detached = new UserData();
        detached.setId(99L);

        User user = new User();
        user.setUsername("asmith");
        user.setEmail("asmith@example.com");
        user.setUserData(detached);

        when(userDataRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.save(user))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    assertThat(rse.getReason()).contains("99");
                });

        verify(userDataRepository).findById(99L);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void save_withUserDataHavingNullId_shouldClearAndPersist() {
        UserData partial = new UserData();
        partial.setId(null);
        partial.setFirstName("ShouldBeIgnored");

        User user = new User();
        user.setUsername("noid");
        user.setEmail("noid@example.com");
        user.setUserData(partial);

        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.save(user);

        verify(userDataRepository, never()).findById(any());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserData()).isNull();
    }

    @Test
    void update_withExistingUserData_shouldResolve() {
        UserData detached = new UserData();
        detached.setId(1L);

        UserData managed = new UserData();
        managed.setId(1L);
        managed.setFirstName("John");

        User user = new User();
        user.setUsername("updated");
        user.setEmail("updated@example.com");
        user.setUserData(detached);

        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(userDataRepository.findById(1L)).thenReturn(Optional.of(managed));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.update(10L, user);

        verify(userDataRepository).findById(1L);
        assertThat(result.getUserData()).isSameAs(managed);
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void update_withNonExistingUserData_shouldThrowNotFound() {
        UserData detached = new UserData();
        detached.setId(88L);

        User user = new User();
        user.setUsername("updated");
        user.setEmail("updated@example.com");
        user.setUserData(detached);

        when(userRepository.findById(7L)).thenReturn(Optional.of(new User()));
        when(userDataRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(7L, user))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_withoutUserData_shouldNotLookup() {
        User user = new User();
        user.setUsername("updated");
        user.setEmail("updated@example.com");
        user.setUserData(null);

        when(userRepository.findById(5L)).thenReturn(Optional.of(new User()));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.update(5L, user);

        verify(userDataRepository, never()).findById(any());
        verify(userRepository).saveAndFlush(any(User.class));
    }
}
