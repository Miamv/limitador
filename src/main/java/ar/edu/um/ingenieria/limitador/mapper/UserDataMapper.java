package ar.edu.um.ingenieria.limitador.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.UserDataDto;

@Mapper(componentModel = "spring")
public interface UserDataMapper {

    UserDataDto toDto(UserData userData);

    @Mapping(target = "user", ignore = true)
    UserData toEntity(UserDataDto dto);

    List<UserDataDto> toDtoList(List<UserData> entities);

    List<UserData> toEntityList(List<UserDataDto> dtos);
}
