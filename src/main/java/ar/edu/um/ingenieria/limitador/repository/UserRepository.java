package ar.edu.um.ingenieria.limitador.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // Fetch-join de userData (incluido en UserDto): evita N+1 al mapear la página.
    // 'roles' no se incluye porque no forma parte del DTO.
    @EntityGraph(attributePaths = "userData")
    @Override
    Page<User> findAll(Pageable pageable);
}
