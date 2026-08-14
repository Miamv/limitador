package ar.edu.um.ingenieria.limitador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
}