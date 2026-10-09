package ar.edu.um.ingenieria.limitador.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserDto;

@Mapper(componentModel = "spring", uses = UserDataMapper.class)
public interface UserMapper {

    UserDto toDto(User user);

    // 'roles' no forma parte del DTO: hoy está @JsonIgnore en ambas direcciones
    // en la entidad. Exponerlo requeriría resolver roles existentes en el servicio.
    @Mapping(target = "roles", ignore = true)
    User toEntity(UserDto dto);

    List<UserDto> toDtoList(List<User> users);

    List<User> toEntityList(List<UserDto> dtos);
}
