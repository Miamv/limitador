package ar.edu.um.ingenieria.limitador.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ar.edu.um.ingenieria.limitador.dto.AuthResponse;
import ar.edu.um.ingenieria.limitador.dto.LoginRequest;
import ar.edu.um.ingenieria.limitador.services.AuthenticationService;
import ar.edu.um.ingenieria.limitador.services.LoginAuditService;
import ar.edu.um.ingenieria.limitador.util.ClientIpResolver;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final AuthenticationService authenticationService;
    private final LoginAuditService loginAuditService;
    private final ClientIpResolver clientIpResolver;

    public LoginController(
            AuthenticationService authenticationService,
            LoginAuditService loginAuditService,
            ClientIpResolver clientIpResolver) {
        this.authenticationService = authenticationService;
        this.loginAuditService = loginAuditService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.extractClientIp(httpRequest);
        String username = (request != null) ? request.username() : null;

        try {
            AuthResponse response = authenticationService.login(request);
            loginAuditService.recordLoginAttempt(username, clientIp, true, null);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            loginAuditService.recordLoginAttempt(username, clientIp, false, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            loginAuditService.recordLoginAttempt(username, clientIp, false, ex.getMessage());
            throw ex;
        }
    }
}
