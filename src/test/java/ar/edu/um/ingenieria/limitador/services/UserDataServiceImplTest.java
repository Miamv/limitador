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

import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;

@ExtendWith(MockitoExtension.class)
class UserDataServiceImplTest {

    @Mock
    private UserDataRepository userDataRepository;

    @InjectMocks
    private UserDataServiceImpl userDataService;

    private UserData userData(String firstName) {
        UserData ud = new UserData();
        ud.setFirstName(firstName);
        ud.setPhoneNumber("555-1234");
        return ud;
    }

    @Test
    void findAll_shouldReturnAllUserData() {
        UserData alice = userData("Alice");
        UserData bob = userData("Bob");
        when(userDataRepository.findAll()).thenReturn(List.of(alice, bob));

        List<UserData> result = userDataService.findAll();

        assertThat(result).containsExactly(alice, bob);
    }

    @Test
    void findById_whenPresent_shouldReturnUserData() {
        UserData alice = userData("Alice");
        when(userDataRepository.findById(1L)).thenReturn(Optional.of(alice));

        Optional<UserData> result = userDataService.findById(1L);

        assertThat(result).contains(alice);
    }

    @Test
    void findById_whenAbsent_shouldReturnEmpty() {
        when(userDataRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<UserData> result = userDataService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldPersistUserData() {
        UserData alice = userData("Alice");
        when(userDataRepository.saveAndFlush(any(UserData.class))).thenReturn(alice);

        UserData result = userDataService.save(alice);

        assertThat(result).isSameAs(alice);
        verify(userDataRepository).saveAndFlush(alice);
    }

    @Test
    void update_withExistingId_shouldSetIdAndPersist() {
        UserData alice = userData("Alice");
        when(userDataRepository.findById(10L)).thenReturn(Optional.of(userData("Existing")));
        when(userDataRepository.saveAndFlush(any(UserData.class))).thenAnswer(i -> i.getArgument(0));

        UserData result = userDataService.update(10L, alice);

        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(userDataRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void update_withMissingId_shouldThrow() {
        UserData alice = userData("Alice");
        when(userDataRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDataService.update(7L, alice))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("7");
    }

    @Test
    void deleteById_withExistingId_shouldDelete() {
        when(userDataRepository.findById(5L)).thenReturn(Optional.of(userData("Alice")));

        userDataService.deleteById(5L);

        verify(userDataRepository).deleteById(5L);
    }

    @Test
    void deleteById_withMissingId_shouldThrow() {
        when(userDataRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDataService.deleteById(4L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("4");
    }
}