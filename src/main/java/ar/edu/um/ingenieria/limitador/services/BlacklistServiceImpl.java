package ar.edu.um.ingenieria.limitador.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.repository.BlacklistRuleRepository;
import ar.edu.um.ingenieria.limitador.util.IpCidrValidator;

@Service
public class BlacklistServiceImpl implements BlacklistService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistServiceImpl.class);

    private final BlacklistRuleRepository blacklistRuleRepository;
    private final Map<String, IpAddressMatcher> matcherCache = new ConcurrentHashMap<>();

    public BlacklistServiceImpl(BlacklistRuleRepository blacklistRuleRepository) {
        this.blacklistRuleRepository = blacklistRuleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlacklistRule> findMatchingRule(String clientIp, String username) {
        LocalDateTime now = LocalDateTime.now();
        List<BlacklistRule> candidates = blacklistRuleRepository.findActiveRulesCandidates(username, now);

        for (BlacklistRule rule : candidates) {
            if (!rule.isEffective()) {
                continue;
            }

            // 1. Verificación por Usuario
            if (rule.getUsername() != null && !rule.getUsername().isBlank()
                    && username != null && !username.isBlank()) {
                if (rule.getUsername().trim().equalsIgnoreCase(username.trim())) {
                    log.warn("Acceso BLOQUEADO por regla de Usuario [id: {}, user: '{}'] motivo: '{}'",
                            rule.getId(), rule.getUsername(), rule.getReason());
                    return Optional.of(rule);
                }
            }

            // 2. Verificación por IP / CIDR (IPv4 o IPv6)
            if (rule.getIpOrCidr() != null && !rule.getIpOrCidr().isBlank()
                    && clientIp != null && !clientIp.isBlank()) {
                if (matchesIp(rule.getIpOrCidr().trim(), clientIp.trim())) {
                    log.warn("Acceso BLOQUEADO por regla de IP/CIDR [id: {}, rule: '{}', ip: '{}'] motivo: '{}'",
                            rule.getId(), rule.getIpOrCidr(), clientIp, rule.getReason());
                    return Optional.of(rule);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String clientIp, String username) {
        return findMatchingRule(clientIp, username).isPresent();
    }

    @Override
    @Transactional
    public BlacklistRule createRule(BlacklistRule rule) {
        validateRule(rule);
        return blacklistRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public BlacklistRule updateRule(Long id, BlacklistRule rule) {
        BlacklistRule existing = blacklistRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada con id: " + id));

        validateRule(rule);
        existing.setIpOrCidr(rule.getIpOrCidr());
        existing.setUsername(rule.getUsername());
        existing.setReason(rule.getReason());
        existing.setActive(rule.isActive());
        existing.setExpiresAt(rule.getExpiresAt());

        matcherCache.remove(id.toString());
        return blacklistRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        if (!blacklistRuleRepository.existsById(id)) {
            throw new IllegalArgumentException("Regla no encontrada con id: " + id);
        }
        blacklistRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlacklistRule> findAll() {
        return blacklistRuleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlacklistRule> findById(Long id) {
        return blacklistRuleRepository.findById(id);
    }

    private boolean matchesIp(String ruleIpOrCidr, String clientIp) {
        if (!IpCidrValidator.isValidIp(clientIp)) {
            return false;
        }
        try {
            IpAddressMatcher matcher = matcherCache.computeIfAbsent(ruleIpOrCidr, IpAddressMatcher::new);
            return matcher.matches(clientIp);
        } catch (Exception e) {
            log.warn("Error evaluando matcher de IP [regla: '{}', ip: '{}']: {}", ruleIpOrCidr, clientIp, e.getMessage());
            return false;
        }
    }

    private void validateRule(BlacklistRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("La regla no puede ser nula");
        }
        boolean hasIp = rule.getIpOrCidr() != null && !rule.getIpOrCidr().isBlank();
        boolean hasUser = rule.getUsername() != null && !rule.getUsername().isBlank();
        if (!hasIp && !hasUser) {
            throw new IllegalArgumentException("Debe especificar al menos una IP/CIDR o un nombre de usuario");
        }
        if (hasIp && !IpCidrValidator.isValidIpOrCidr(rule.getIpOrCidr())) {
            throw new IllegalArgumentException("Formato de IP/CIDR inválido: " + rule.getIpOrCidr());
        }
    }
}
