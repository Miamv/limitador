package ar.edu.um.ingenieria.limitador.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import ar.edu.um.ingenieria.limitador.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryFindByUsernameTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByUsername_whenExists_returnsUserWithItsData() {
		var user = new User();
		user.setUsername("jperez");
		user.setEmail("jperez@um.edu.ar");
		user.setActive(true);
		user.setPassword("$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		userRepository.saveAndFlush(user);

		var found = userRepository.findByUsername("jperez");

		assertThat(found).isPresent();
		assertThat(found.get().getUsername()).isEqualTo("jperez");
		assertThat(found.get().getEmail()).isEqualTo("jperez@um.edu.ar");
		assertThat(found.get().getActive()).isTrue();
		assertThat(found.get().getPassword()).startsWith("$2a$10$");
	}

	@Test
	void findByUsername_whenNotExists_returnsEmpty() {
		assertThat(userRepository.findByUsername("noexiste")).isEmpty();
	}
}
