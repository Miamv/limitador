package ar.edu.um.ingenieria.limitador.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.RegisterRequest;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "userData", ignore = true)
    @Mapping(target = "active", ignore = true)
    User toUser(RegisterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserData toUserData(RegisterRequest request);
}
