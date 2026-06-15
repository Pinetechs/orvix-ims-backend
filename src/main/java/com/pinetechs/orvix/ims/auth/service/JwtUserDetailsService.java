package com.pinetechs.orvix.ims.auth.service;

import com.pinetechs.orvix.ims.security.JwtUserDetails;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JwtUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCaseAndDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return new JwtUserDetails(user);
    }
}
