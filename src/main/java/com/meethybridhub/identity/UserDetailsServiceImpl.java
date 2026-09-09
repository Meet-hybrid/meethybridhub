package com.meethybridhub.identity;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(this::buildUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }


    private UserDetails buildUserDetails(User user) {
        return new AppUser(user);
    }


    public UserDetails loadUserForAuthentication(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));


        if (!user.isEmailVerified()) {
            throw new UsernameNotFoundException("Email not verified for user: " + email);
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("Account is not active: " + user.getStatus());
        }

        return buildUserDetails(user);
    }
}
