package ar.edu.um.ingenieria.limitador.dto;

import java.time.LocalDateTime;

public record BlacklistRuleDto(
        Long id,
        String ipOrCidr,
        String username,
        String reason,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
