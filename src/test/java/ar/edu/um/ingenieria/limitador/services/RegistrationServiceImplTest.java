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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.RegisterRequest;
import ar.edu.um.ingenieria.limitador.exceptions.DuplicateUserException;
import ar.edu.um.ingenieria.limitador.mapper.AuthMapper;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserDataService userDataService;

    @Mock
    private UserService userService;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private RegisterRequest request() {
        return new RegisterRequest("jperez", "jperez@um.edu.ar", "Secreto123",
                "Juan", "Perez", "+5492615551234", "Calle Falsa 123");
    }

    private UserData userDataEntity() {
        UserData userData = new UserData();
        userData.setFirstName("Juan");
        userData.setLastName("Perez");
        userData.setPhoneNumber("+5492615551234");
        userData.setAddress("Calle Falsa 123");
        return userData;
    }

    private User userEntity() {
        User user = new User();
        user.setUsername("jperez");
        user.setEmail("jperez@um.edu.ar");
        user.setPassword("Secreto123");
        return user;
    }

    @Test
    void register_withValidData_shouldPersistUserDataAndUserInOrder() {
        UserData userData = userDataEntity();
        User user = userEntity();
        Role userRole = new Role("USER");

        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.existsByEmail("jperez@um.edu.ar")).thenReturn(false);
        when(authMapper.toUserData(any(RegisterRequest.class))).thenReturn(userData);
        when(userDataService.save(any(UserData.class))).thenAnswer(i -> {
            UserData saved = i.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(authMapper.toUser(any(RegisterRequest.class))).thenReturn(user);
        when(roleRepository.findByDescription("USER")).thenReturn(Optional.of(userRole));
        when(userService.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = registrationService.register(request());

        InOrder inOrder = org.mockito.Mockito.inOrder(userDataService, userService);
        inOrder.verify(userDataService).save(any(UserData.class));
        inOrder.verify(userService).save(any(User.class));

        assertThat(result.getUserData()).isSameAs(userData);
        assertThat(result.getUserData().getId()).isEqualTo(10L);
        assertThat(result.getActive()).isTrue();
        assertThat(result.getRoles()).containsExactly(userRole);
    }

    @Test
    void register_withExistingUsername_shouldThrow409AndNotPersist() {
        when(userRepository.existsByUsername("jperez")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request()))
                .isInstanceOf(DuplicateUserException.class)
                .satisfies(ex -> {
                    DuplicateUserException dup = (DuplicateUserException) ex;
                    assertThat(dup.getField()).isEqualTo("username");
                    assertThat(dup.getMessage()).contains("jperez");
                });

        verify(userDataService, never()).save(any());
        verify(userService, never()).save(any());
    }

    @Test
    void register_withExistingEmail_shouldThrow409AndNotPersist() {
        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.existsByEmail("jperez@um.edu.ar")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request()))
                .isInstanceOf(DuplicateUserException.class)
                .satisfies(ex -> assertThat(((DuplicateUserException) ex).getField()).isEqualTo("email"));

        verify(userDataService, never()).save(any());
        verify(userService, never()).save(any());
    }

    @Test
    void register_whenUserRoleExists_shouldAssignIt() {
        UserData userData = userDataEntity();
        User user = userEntity();
        Role existing = new Role("USER");

        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.existsByEmail("jperez@um.edu.ar")).thenReturn(false);
        when(authMapper.toUserData(any(RegisterRequest.class))).thenReturn(userData);
        when(userDataService.save(any(UserData.class))).thenAnswer(i -> i.getArgument(0));
        when(authMapper.toUser(any(RegisterRequest.class))).thenReturn(user);
        when(roleRepository.findByDescription("USER")).thenReturn(Optional.of(existing));
        when(userService.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        registrationService.register(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        assertThat(captor.getValue().getRoles()).containsExactly(existing);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void register_whenUserRoleDoesNotExist_shouldCreateAndAssignIt() {
        UserData userData = userDataEntity();
        User user = userEntity();
        Role created = new Role("USER");
        created.setId(3L);

        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.existsByEmail("jperez@um.edu.ar")).thenReturn(false);
        when(authMapper.toUserData(any(RegisterRequest.class))).thenReturn(userData);
        when(userDataService.save(any(UserData.class))).thenAnswer(i -> i.getArgument(0));
        when(authMapper.toUser(any(RegisterRequest.class))).thenReturn(user);
        when(roleRepository.findByDescription("USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(created);
        when(userService.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        registrationService.register(request());

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getDescription()).isEqualTo("USER");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(created);
    }

    @Test
    void register_shouldDelegatePasswordEncodingToUserService() {
        UserData userData = userDataEntity();
        User user = userEntity();

        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.existsByEmail("jperez@um.edu.ar")).thenReturn(false);
        when(authMapper.toUserData(any(RegisterRequest.class))).thenReturn(userData);
        when(userDataService.save(any(UserData.class))).thenAnswer(i -> i.getArgument(0));
        when(authMapper.toUser(any(RegisterRequest.class))).thenReturn(user);
        when(roleRepository.findByDescription("USER")).thenReturn(Optional.of(new Role("USER")));
        when(userService.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        registrationService.register(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("Secreto123");
    }

    @Test
    void register_shouldAssociateUserDataWithUser() {
        UserData userData = userDataEntity();
        userData.setId(10L);
        User user = userEntity();

        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.existsByEmail("jperez@um.edu.ar")).thenReturn(false);
        when(authMapper.toUserData(any(RegisterRequest.class))).thenReturn(userData);
        when(userDataService.save(any(UserData.class))).thenReturn(userData);
        when(authMapper.toUser(any(RegisterRequest.class))).thenReturn(user);
        when(roleRepository.findByDescription("USER")).thenReturn(Optional.of(new Role("USER")));
        when(userService.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = registrationService.register(request());

        assertThat(result.getUserData()).isSameAs(userData);
        assertThat(result.getUserData().getId()).isEqualTo(10L);
    }
}
