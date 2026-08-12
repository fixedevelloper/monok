package com.monokek.identity.application;

import com.monokek.identity.UserDirectory;
import com.monokek.identity.domain.User;
import com.monokek.identity.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class UserDirectoryService implements UserDirectory {

    private final UserRepository userRepository;

    UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> namesByIds(Collection<Long> ids) {
        return userRepository.findByIdIn(ids).stream().collect(Collectors.toMap(User::getId, User::getName));
    }
}
