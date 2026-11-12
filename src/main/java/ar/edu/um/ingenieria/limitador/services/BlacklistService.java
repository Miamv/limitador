package ar.edu.um.ingenieria.limitador.services;

import java.util.List;
import java.util.Optional;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;

public interface BlacklistService {

    Optional<BlacklistRule> findMatchingRule(String clientIp, String username);

    boolean isBlacklisted(String clientIp, String username);

    BlacklistRule createRule(BlacklistRule rule);

    BlacklistRule updateRule(Long id, BlacklistRule rule);

    void deleteRule(Long id);

    List<BlacklistRule> findAll();

    Optional<BlacklistRule> findById(Long id);
}
