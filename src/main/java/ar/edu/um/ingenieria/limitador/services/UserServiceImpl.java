package ar.edu.um.ingenieria.limitador.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;

    public UserServiceImpl(UserRepository userRepository, UserDataRepository userDataRepository) {
        this.userRepository = userRepository;
        this.userDataRepository = userDataRepository;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        resolveUserData(user);
        return userRepository.saveAndFlush(user);
    }

    @Override
    public User update(Long id, User user) {
        if (!userRepository.findById(id).isPresent()) {
            throw new RuntimeException("User not found with id: " + id);
        }
        resolveUserData(user);
        user.setId(id);
        return userRepository.saveAndFlush(user);
    }

    private void resolveUserData(User user) {
        if (user.getUserData() == null) {
            return;
        }
        Long dataId = user.getUserData().getId();
        if (dataId == null) {
            user.setUserData(null);
            return;
        }
        UserData managed = userDataRepository.findById(dataId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "UserData not found with id: " + dataId));
        user.setUserData(managed);
    }

    @Override
    public void deleteById(Long id) {
        if (!userRepository.findById(id).isPresent()) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
