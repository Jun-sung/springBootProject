package com.safehome.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
	
	
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

	    http
	        .authorizeHttpRequests(auth -> auth
	        		.requestMatchers(
	        				  "/", "/user/**", "/map/**", "/qna/**",
	        				  "/api/map/**", "/api/chat",
	        				  "/css/**", "/js/**","/error"
	        				).permitAll()
	        	.requestMatchers("/admin/**").hasRole("ADMIN")
	            .requestMatchers("/mypage/**").hasRole("USER")
	            .anyRequest().authenticated()
	        )
	        .formLogin(form -> form
	            .loginProcessingUrl("/login")
	            .successHandler((request, response, authentication) -> {
	                response.setStatus(HttpServletResponse.SC_OK);
	            })
	            .failureHandler((request, response, exception) -> {
	                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	            })
	        )
	        .logout(logout -> logout
	            .logoutUrl("/logout")
	            .invalidateHttpSession(true)
	            .deleteCookies("JSESSIONID")
	            .logoutSuccessUrl("/")
	            
	        )
	        .userDetailsService(userDetailsService)
	        .csrf(csrf -> csrf.disable()); // AJAX 간단 구현용 (운영 시 토큰 처리 권장)

	    return http.build();
	}
}