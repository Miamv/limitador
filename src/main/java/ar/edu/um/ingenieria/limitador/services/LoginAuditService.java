package ar.edu.um.ingenieria.limitador.services;

import ar.edu.um.ingenieria.limitador.domain.LoginAudit;

public interface LoginAuditService {

    LoginAudit recordLoginAttempt(String username, String ipAddress, boolean success, String failureReason);
}
