package ar.edu.um.ingenieria.limitador.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import ar.edu.um.ingenieria.limitador.domain.UserData;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserDataRepositoryTest {

    @Autowired
    private UserDataRepository userDataRepository;

    @Test
    void persistsUserDataAndFindsById() {
        var userData = new UserData();
        userData.setFirstName("Alice");
        userData.setLastName("Smith");
        userData.setAddress("123 Main St");
        userData.setPhoneNumber("555-1234");

        UserData saved = userDataRepository.save(userData);

        assertThat(saved.getId()).isNotNull();

        var found = userDataRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getFirstName()).isEqualTo("Alice");
        assertThat(found.getLastName()).isEqualTo("Smith");
        assertThat(found.getAddress()).isEqualTo("123 Main St");
        assertThat(found.getPhoneNumber()).isEqualTo("555-1234");
    }

    @Test
    void persistsMultipleUserDataAndFindsAll() {
        var data1 = new UserData();
        data1.setFirstName("Alice");
        data1.setPhoneNumber("555-1234");

        var data2 = new UserData();
        data2.setFirstName("Bob");
        data2.setPhoneNumber("555-5678");

        userDataRepository.save(data1);
        userDataRepository.save(data2);

        var all = userDataRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void deletesUserDataById() {
        var userData = new UserData();
        userData.setFirstName("Alice");
        userData.setPhoneNumber("555-1234");

        UserData saved = userDataRepository.save(userData);
        Long id = saved.getId();

        userDataRepository.deleteById(id);

        assertThat(userDataRepository.findById(id)).isEmpty();
    }
}
