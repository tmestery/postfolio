package com.postfolio.postfolio.models.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fully qualify the entity User to avoid conflict
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
    }
}