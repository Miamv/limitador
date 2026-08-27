package ar.edu.um.ingenieria.limitador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.UserData;

public interface UserDataRepository extends JpaRepository<UserData, Long> {
}
