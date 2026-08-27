package ar.edu.um.ingenieria.limitador.services;

import java.util.List;
import java.util.Optional;

import ar.edu.um.ingenieria.limitador.domain.User;

public interface UserService {
    List<User> findAll();
    Optional<User> findById(Long id);
    User save(User user);
    User update(Long id, User user);
    void deleteById(Long id);
}
