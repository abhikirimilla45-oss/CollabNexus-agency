package com.collabnexus.security;

import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CollabNexusUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CollabNexusUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for this email address"));
        return new SecurityUser(user);
    }
}
