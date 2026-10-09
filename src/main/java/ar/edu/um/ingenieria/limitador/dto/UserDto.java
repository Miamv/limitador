package ar.edu.um.ingenieria.limitador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto(Long id, String username, String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password, Boolean active,
        UserDataDto userData) {
}
