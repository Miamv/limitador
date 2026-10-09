package ar.edu.um.ingenieria.limitador.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.dto.RoleDto;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleDto toDto(Role role);

    // La back-reference 'users' no forma parte del DTO (evita recursión infinita).
    @Mapping(target = "users", ignore = true)
    Role toEntity(RoleDto dto);

    List<RoleDto> toDtoList(List<Role> roles);

    List<Role> toEntityList(List<RoleDto> dtos);
}
