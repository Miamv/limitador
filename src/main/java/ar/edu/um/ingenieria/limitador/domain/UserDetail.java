package ar.edu.um.ingenieria.limitador.domain;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Vista inmutable y de solo lectura de un {@link User}.
 * No incluye datos sensibles (password).
 */
public record UserDetail(Long id, String username, String email, boolean active, Set<String> roles) {

    public UserDetail {
        roles = roles == null
                ? Collections.emptySortedSet()
                : Collections.unmodifiableSortedSet(new TreeSet<>(roles));
    }
}
