package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void findAll_shouldReturnAllRoles() {
        Role admin = new Role("ADMIN");
        Role manager = new Role("MANAGER");
        when(roleRepository.findAll()).thenReturn(List.of(admin, manager));

        List<Role> roles = roleService.findAll();

        assertThat(roles).containsExactly(admin, manager);
    }

    @Test
    void findById_whenPresent_shouldReturnRole() {
        Role admin = new Role("ADMIN");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));

        Optional<Role> result = roleService.findById(1L);

        assertThat(result).contains(admin);
    }

    @Test
    void findById_whenAbsent_shouldReturnEmpty() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Role> result = roleService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldPersistRole() {
        Role role = new Role("ADMIN");
        when(roleRepository.saveAndFlush(any(Role.class))).thenReturn(role);

        Role result = roleService.save(role);

        assertThat(result).isSameAs(role);
        verify(roleRepository).saveAndFlush(role);
    }

    @Test
    void update_withExistingId_shouldSetIdAndPersist() {
        Role role = new Role("MANAGER");
        when(roleRepository.findById(10L)).thenReturn(Optional.of(new Role()));
        when(roleRepository.saveAndFlush(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = roleService.update(10L, role);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void update_withMissingId_shouldThrow() {
        Role role = new Role("MANAGER");
        when(roleRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.update(7L, role))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("7");
    }

    @Test
    void deleteById_withExistingId_shouldDelete() {
        when(roleRepository.findById(5L)).thenReturn(Optional.of(new Role()));

        roleService.deleteById(5L);

        verify(roleRepository).deleteById(5L);
    }

    @Test
    void deleteById_withMissingId_shouldThrow() {
        when(roleRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.deleteById(4L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("4");
    }
}