package ar.edu.um.ingenieria.limitador.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.dto.BlacklistRuleDto;
import ar.edu.um.ingenieria.limitador.mapper.BlacklistRuleMapper;
import ar.edu.um.ingenieria.limitador.services.BlacklistService;

@RestController
@RequestMapping("/api/blacklist")
public class BlacklistController {

    private final BlacklistService blacklistService;
    private final BlacklistRuleMapper blacklistRuleMapper;

    public BlacklistController(BlacklistService blacklistService, BlacklistRuleMapper blacklistRuleMapper) {
        this.blacklistService = blacklistService;
        this.blacklistRuleMapper = blacklistRuleMapper;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<List<BlacklistRuleDto>> findAll() {
        return ResponseEntity.ok(blacklistRuleMapper.toDtoList(blacklistService.findAll()));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<BlacklistRuleDto> findById(@PathVariable Long id) {
        return blacklistService.findById(id)
                .map(blacklistRuleMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<BlacklistRuleDto> create(@RequestBody BlacklistRuleDto dto) {
        BlacklistRule rule = blacklistRuleMapper.toEntity(dto);
        if (dto != null && dto.active() != null) {
            rule.setActive(dto.active());
        }
        BlacklistRule created = blacklistService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(blacklistRuleMapper.toDto(created));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BlacklistRuleDto> update(@PathVariable Long id, @RequestBody BlacklistRuleDto dto) {
        if (blacklistService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BlacklistRule rule = blacklistRuleMapper.toEntity(dto);
        if (dto != null && dto.active() != null) {
            rule.setActive(dto.active());
        }
        BlacklistRule updated = blacklistService.updateRule(id, rule);
        return ResponseEntity.ok(blacklistRuleMapper.toDto(updated));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (blacklistService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        blacklistService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
