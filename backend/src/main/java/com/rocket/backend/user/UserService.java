package com.rocket.backend.user;

import com.rocket.backend.common.GameException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new GameException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "No user " + id));
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}
