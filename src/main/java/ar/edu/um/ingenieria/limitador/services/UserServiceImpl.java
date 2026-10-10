package ar.edu.um.ingenieria.limitador.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserDataRepository userDataRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userDataRepository = userDataRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findPaged(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        user.setPassword(encodeIfNecessary(user.getPassword()));
        resolveUserData(user);
        return userRepository.saveAndFlush(user);
    }

    @Override
    public User update(Long id, User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setPassword(user.getPassword() == null
                ? existing.getPassword()
                : encodeIfNecessary(user.getPassword()));
        resolveUserData(user);
        user.setId(id);
        return userRepository.saveAndFlush(user);
    }

    private String encodeIfNecessary(String password) {
        if (password == null || isAlreadyEncoded(password)) {
            return password;
        }
        return passwordEncoder.encode(password);
    }

    private static boolean isAlreadyEncoded(String password) {
        return BCRYPT_PATTERN.matcher(password).matches();
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
