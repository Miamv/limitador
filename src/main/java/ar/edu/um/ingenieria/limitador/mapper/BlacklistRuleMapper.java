package ar.edu.um.ingenieria.limitador.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.dto.BlacklistRuleDto;

@Mapper(componentModel = "spring")
public interface BlacklistRuleMapper {

    BlacklistRuleDto toDto(BlacklistRule rule);

    BlacklistRule toEntity(BlacklistRuleDto dto);

    List<BlacklistRuleDto> toDtoList(List<BlacklistRule> rules);

    List<BlacklistRule> toEntityList(List<BlacklistRuleDto> dtos);
}
