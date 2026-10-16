package ar.edu.um.ingenieria.limitador.dto;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import ar.edu.um.ingenieria.limitador.domain.UserDetail;

public record AuthResponse(String token, Long id, String username, String email, boolean active, Set<String> roles) {

    public AuthResponse {
        roles = roles == null
                ? Collections.emptySortedSet()
                : Collections.unmodifiableSortedSet(new TreeSet<>(roles));
    }

    public static AuthResponse of(String token, UserDetail detail) {
        return new AuthResponse(token, detail.id(), detail.username(), detail.email(), detail.active(),
                detail.roles());
    }
}
