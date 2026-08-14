package ar.edu.um.ingenieria.limitador.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRoleRelationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	void userAssignedRoleRoundTripsThroughPersistence() {
		Role admin = roleRepository.save(new Role("Administrador"));
		User user = new User();
		user.setUsername("jperez");
		user.setEmail("jperez@um.edu.ar");
		user.setActive(true);
		user.getRoles().add(admin);

		userRepository.save(user);

		User loaded = userRepository.findById(user.getId()).orElseThrow();
		assertThat(loaded.getRoles()).extracting(Role::getDescription).containsExactly("Administrador");
	}
}