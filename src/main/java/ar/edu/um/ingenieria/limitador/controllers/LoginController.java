package ar.edu.um.ingenieria.limitador.controllers;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.LoginStatus;
import ar.edu.um.ingenieria.limitador.dto.AuthResponse;
import ar.edu.um.ingenieria.limitador.dto.LoginRequest;
import ar.edu.um.ingenieria.limitador.exceptions.BlacklistedException;
import ar.edu.um.ingenieria.limitador.services.AuthenticationService;
import ar.edu.um.ingenieria.limitador.services.BlacklistService;
import ar.edu.um.ingenieria.limitador.services.LoginAuditService;
import ar.edu.um.ingenieria.limitador.util.ClientIpResolver;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final AuthenticationService authenticationService;
    private final LoginAuditService loginAuditService;
    private final ClientIpResolver clientIpResolver;
    private final BlacklistService blacklistService;

    public LoginController(
            AuthenticationService authenticationService,
            LoginAuditService loginAuditService,
            ClientIpResolver clientIpResolver,
            BlacklistService blacklistService) {
        this.authenticationService = authenticationService;
        this.loginAuditService = loginAuditService;
        this.clientIpResolver = clientIpResolver;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.extractClientIp(httpRequest);
        String username = (request != null) ? request.username() : null;

        // Evaluar blacklist (IP/CIDR y usuario) antes de autenticar o generar JWT
        Optional<BlacklistRule> matchingRule = blacklistService.findMatchingRule(clientIp, username);
        if (matchingRule.isPresent()) {
            BlacklistRule rule = matchingRule.get();
            String reason = (rule.getReason() != null && !rule.getReason().isBlank())
                    ? rule.getReason()
                    : "Acceso bloqueado por lista negra";
            loginAuditService.recordLoginAttempt(username, clientIp, LoginStatus.BLOCKED, reason);
            throw new BlacklistedException(reason);
        }

        try {
            AuthResponse response = authenticationService.login(request);
            loginAuditService.recordLoginAttempt(username, clientIp, LoginStatus.SUCCESS, null);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            loginAuditService.recordLoginAttempt(username, clientIp, LoginStatus.FAILURE, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            loginAuditService.recordLoginAttempt(username, clientIp, LoginStatus.FAILURE, ex.getMessage());
            throw ex;
        }
    }
}
