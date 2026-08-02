package com.postfolio.postfolio.models.user;

import com.postfolio.postfolio.models.follow.FollowService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository repository;
    private final FollowService followService;

    public UserService(UserRepository repository, FollowService followService) {
        this.repository = repository;
        this.followService = followService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<WebUser> optionalUser = repository.findByUsername(username);

        if (optionalUser.isPresent()) {
            WebUser userEntity = optionalUser.get();
            return User.withUsername(userEntity.getUsername())
                    .password(userEntity.getPassword())
                    .roles("USER")
                    .build();
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    @Transactional
    public void setUserAccountStatus(Long userId, boolean accountPublic) {
        repository.setAccountStatus(userId, accountPublic);
        if (accountPublic) {
            repository.findById(userId).ifPresent(followService::autoAcceptPendingFor);
        }
    }
}