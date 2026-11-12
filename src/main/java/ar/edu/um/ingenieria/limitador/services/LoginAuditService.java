package ar.edu.um.ingenieria.limitador.services;

import ar.edu.um.ingenieria.limitador.domain.LoginAudit;
import ar.edu.um.ingenieria.limitador.domain.LoginStatus;

public interface LoginAuditService {

    LoginAudit recordLoginAttempt(String username, String ipAddress, LoginStatus status, String failureReason);

    default LoginAudit recordLoginAttempt(String username, String ipAddress, boolean success, String failureReason) {
        return recordLoginAttempt(username, ipAddress, success ? LoginStatus.SUCCESS : LoginStatus.FAILURE, failureReason);
    }
}
