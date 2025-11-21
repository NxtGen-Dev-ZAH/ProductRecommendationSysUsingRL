package com.datasaz.ecommerce.configs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

// del old version
@TestConfiguration
public class TestSecurityConfig {
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("testuser")
                .password("{noop}testpass")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}