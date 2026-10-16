package ar.edu.um.ingenieria.limitador.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	// Fetch-join de roles: getAuthorities() (UserDetails) debe poder leerse
	// después de cerrar la sesión de la consulta, fuera de toda transacción.
	@EntityGraph(attributePaths = "roles")
	Optional<User> findByUsername(String username);

    // Fetch-join de userData (incluido en UserDto): evita N+1 al mapear la página.
    // 'roles' no se incluye porque no forma parte del DTO.
    @EntityGraph(attributePaths = "userData")
    @Override
    Page<User> findAll(Pageable pageable);
}
