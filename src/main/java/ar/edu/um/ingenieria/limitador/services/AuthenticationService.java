package ar.edu.um.ingenieria.limitador.services;

import ar.edu.um.ingenieria.limitador.dto.AuthResponse;
import ar.edu.um.ingenieria.limitador.dto.LoginRequest;

public interface AuthenticationService {
    AuthResponse login(LoginRequest request);
}
