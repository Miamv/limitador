package ar.edu.um.ingenieria.limitador.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.mapper.BlacklistRuleMapperImpl;
import ar.edu.um.ingenieria.limitador.services.BlacklistService;

@WebMvcTest(BlacklistController.class)
@Import(BlacklistRuleMapperImpl.class)
class BlacklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlacklistService blacklistService;

    private BlacklistRule createIpRule(Long id, String ipOrCidr, String reason) {
        BlacklistRule rule = BlacklistRule.forIpOrCidr(ipOrCidr, reason);
        rule.setId(id);
        return rule;
    }

    private BlacklistRule createUserRule(Long id, String username, String reason) {
        BlacklistRule rule = BlacklistRule.forUsername(username, reason);
        rule.setId(id);
        return rule;
    }

    @Test
    void findAll_shouldReturnAllRules() throws Exception {
        when(blacklistService.findAll()).thenReturn(List.of(
                createIpRule(1L, "192.168.1.50", "Malicious host"),
                createUserRule(2L, "malicious_user", "Suspicious activity")
        ));

        mockMvc.perform(get("/api/blacklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ipOrCidr").value("192.168.1.50"))
                .andExpect(jsonPath("$[0].reason").value("Malicious host"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("malicious_user"))
                .andExpect(jsonPath("$[1].reason").value("Suspicious activity"));
    }

    @Test
    void findById_whenRuleExists_shouldReturnIt() throws Exception {
        when(blacklistService.findById(1L)).thenReturn(Optional.of(createIpRule(1L, "192.168.1.50", "Host banned")));

        mockMvc.perform(get("/api/blacklist/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ipOrCidr").value("192.168.1.50"))
                .andExpect(jsonPath("$.reason").value("Host banned"));
    }

    @Test
    void findById_whenRuleDoesNotExist_shouldReturnNotFound() throws Exception {
        when(blacklistService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/blacklist/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withValidIpRule_shouldReturnCreated() throws Exception {
        when(blacklistService.createRule(any(BlacklistRule.class))).thenReturn(createIpRule(1L, "10.0.0.0/24", "Internal subnet banned"));

        mockMvc.perform(post("/api/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ipOrCidr": "10.0.0.0/24",
                                    "reason": "Internal subnet banned",
                                    "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ipOrCidr").value("10.0.0.0/24"))
                .andExpect(jsonPath("$.reason").value("Internal subnet banned"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_withValidUserRule_shouldReturnCreated() throws Exception {
        when(blacklistService.createRule(any(BlacklistRule.class))).thenReturn(createUserRule(2L, "hacker", "Account blocked"));

        mockMvc.perform(post("/api/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "hacker",
                                    "reason": "Account blocked"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("hacker"))
                .andExpect(jsonPath("$.reason").value("Account blocked"));
    }

    @Test
    void create_withInvalidIpOrCidr_shouldReturnBadRequest() throws Exception {
        when(blacklistService.createRule(any(BlacklistRule.class)))
                .thenThrow(new IllegalArgumentException("Formato de IP/CIDR inválido: invalid_ip"));

        mockMvc.perform(post("/api/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ipOrCidr": "invalid_ip",
                                    "reason": "Bad IP"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Formato de IP/CIDR inválido: invalid_ip"));
    }

    @Test
    void create_withoutIpOrUser_shouldReturnBadRequest() throws Exception {
        when(blacklistService.createRule(any(BlacklistRule.class)))
                .thenThrow(new IllegalArgumentException("Debe especificar al menos una IP/CIDR o un nombre de usuario"));

        mockMvc.perform(post("/api/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "Missing both"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Debe especificar al menos una IP/CIDR o un nombre de usuario"));
    }

    @Test
    void update_whenRuleExists_shouldReturnUpdated() throws Exception {
        when(blacklistService.findById(1L)).thenReturn(Optional.of(createIpRule(1L, "192.168.1.1", "Old reason")));
        when(blacklistService.updateRule(eq(1L), any(BlacklistRule.class)))
                .thenReturn(createIpRule(1L, "192.168.1.1", "Updated reason"));

        mockMvc.perform(put("/api/blacklist/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ipOrCidr": "192.168.1.1",
                                    "reason": "Updated reason",
                                    "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reason").value("Updated reason"));
    }

    @Test
    void update_whenRuleDoesNotExist_shouldReturnNotFound() throws Exception {
        when(blacklistService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/blacklist/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ipOrCidr": "192.168.1.1",
                                    "reason": "Not found"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_whenRuleExists_shouldReturnNoContent() throws Exception {
        when(blacklistService.findById(1L)).thenReturn(Optional.of(createIpRule(1L, "192.168.1.1", "Delete me")));

        mockMvc.perform(delete("/api/blacklist/1"))
                .andExpect(status().isNoContent());

        verify(blacklistService).deleteRule(1L);
    }

    @Test
    void delete_whenRuleDoesNotExist_shouldReturnNotFound() throws Exception {
        when(blacklistService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/blacklist/99"))
                .andExpect(status().isNotFound());
    }
}
