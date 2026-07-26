package Product.Service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthFilter headerAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws jakarta.servlet.ServletException {
        http.csrf(csrf -> csrf.disable())
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/product/health").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/product", "/api/product/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/product").hasRole("SELLER")
                    .requestMatchers(HttpMethod.PUT, "/api/product/**").hasRole("SELLER")
                    .requestMatchers(HttpMethod.DELETE, "/api/product/**").hasRole("SELLER")
                    .anyRequest().authenticated()
                );
        return http.build();
    }
}
