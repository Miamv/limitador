package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.RegisterRequest;
import ar.edu.um.ingenieria.limitador.exceptions.DuplicateUserException;
import ar.edu.um.ingenieria.limitador.mapper.AuthMapperImpl;
import ar.edu.um.ingenieria.limitador.mapper.UserDataMapperImpl;
import ar.edu.um.ingenieria.limitador.mapper.UserMapperImpl;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@DataJpaTest
@Import({ RegistrationServiceImpl.class, UserServiceImpl.class, UserDataServiceImpl.class,
        AuthMapperImpl.class, UserMapperImpl.class, UserDataMapperImpl.class,
        RegistrationIntegrationTest.TestEncoderConfig.class })
class RegistrationIntegrationTest {

    @TestConfiguration
    static class TestEncoderConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest request(String username, String email) {
        return new RegisterRequest(username, email, "Secreto123",
                "Juan", "Perez", "+5492615551234", "Calle Falsa 123");
    }

    @Test
    void register_shouldPersistBothEntitiesLinkedAndHashPassword() {
        User saved = registrationService.register(request("jperez", "jperez@um.edu.ar"));
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUsername()).isEqualTo("jperez");
        assertThat(reloaded.getEmail()).isEqualTo("jperez@um.edu.ar");
        assertThat(reloaded.getActive()).isTrue();
        assertThat(reloaded.getPassword()).isNotEqualTo("Secreto123");
        assertThat(reloaded.getPassword()).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
        assertThat(passwordEncoder.matches("Secreto123", reloaded.getPassword())).isTrue();

        assertThat(reloaded.getUserData()).isNotNull();
        UserData data = reloaded.getUserData();
        assertThat(data.getId()).isNotNull();
        assertThat(data.getFirstName()).isEqualTo("Juan");
        assertThat(data.getLastName()).isEqualTo("Perez");
        assertThat(data.getPhoneNumber()).isEqualTo("+5492615551234");
        assertThat(data.getAddress()).isEqualTo("Calle Falsa 123");

        assertThat(userDataRepository.findById(data.getId())).isPresent();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userDataRepository.count()).isEqualTo(1);
    }

    @Test
    void register_shouldAssignUserRoleCreatingItWhenMissing() {
        User saved = registrationService.register(request("jperez", "jperez@um.edu.ar"));
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getRoles())
                .extracting(role -> role.getDescription())
                .containsExactly("USER");
        assertThat(roleRepository.findAll()).hasSize(1);
    }

    @Test
    void register_withDuplicatedUsername_shouldThrowAndPersistNothing() {
        registrationService.register(request("jperez", "jperez@um.edu.ar"));
        entityManager.clear();
        long usersBefore = userRepository.count();
        long dataBefore = userDataRepository.count();

        assertThatThrownBy(() -> registrationService.register(request("jperez", "otro@um.edu.ar")))
                .isInstanceOf(DuplicateUserException.class)
                .satisfies(ex -> assertThat(((DuplicateUserException) ex).getField()).isEqualTo("username"));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(userDataRepository.count()).isEqualTo(dataBefore);
    }

    @Test
    void register_withDuplicatedEmail_shouldThrowAndPersistNothing() {
        registrationService.register(request("jperez", "jperez@um.edu.ar"));
        entityManager.clear();
        long usersBefore = userRepository.count();
        long dataBefore = userDataRepository.count();

        assertThatThrownBy(() -> registrationService.register(request("otro", "jperez@um.edu.ar")))
                .isInstanceOf(DuplicateUserException.class)
                .satisfies(ex -> assertThat(((DuplicateUserException) ex).getField()).isEqualTo("email"));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(userDataRepository.count()).isEqualTo(dataBefore);
    }

    @Test
    void register_twoDifferentUsers_bothPersistSeparately() {
        registrationService.register(request("jperez", "jperez@um.edu.ar"));
        registrationService.register(request("asmith", "asmith@um.edu.ar"));
        entityManager.clear();

        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(userDataRepository.count()).isEqualTo(2);
        assertThat(userDataRepository.findAll())
                .extracting(UserData::getFirstName)
                .containsOnly("Juan");
    }
}
