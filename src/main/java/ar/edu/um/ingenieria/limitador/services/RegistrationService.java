package ar.edu.um.ingenieria.limitador.services;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.RegisterRequest;

public interface RegistrationService {
    User register(RegisterRequest request);
}
