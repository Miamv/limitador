package ar.edu.um.ingenieria.limitador.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.RegisterRequest;
import ar.edu.um.ingenieria.limitador.exceptions.DuplicateUserException;
import ar.edu.um.ingenieria.limitador.mapper.AuthMapper;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDataService userDataService;
    private final UserService userService;
    private final AuthMapper authMapper;

    public RegistrationServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
            UserDataService userDataService, UserService userService, AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userDataService = userDataService;
        this.userService = userService;
        this.authMapper = authMapper;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("username",
                    "username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("email",
                    "email '" + request.email() + "' is already registered");
        }

        UserData userData = userDataService.save(authMapper.toUserData(request));

        User user = authMapper.toUser(request);
        user.setUserData(userData);
        user.setActive(true);
        user.getRoles().add(findOrCreateDefaultRole());

        return userService.save(user);
    }

    private Role findOrCreateDefaultRole() {
        return roleRepository.findByDescription(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(new Role(DEFAULT_ROLE)));
    }
}
