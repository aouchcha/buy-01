package Product.Service.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                    .antMatchers(HttpMethod.GET, "/api/product/health").permitAll()
                    .antMatchers(HttpMethod.GET, "/api/product", "/api/product/**").permitAll()
                    .antMatchers(HttpMethod.POST, "/api/product").hasRole("SELLER")
                    .antMatchers(HttpMethod.PUT, "/api/product/**").hasRole("SELLER")
                    .antMatchers(HttpMethod.DELETE, "/api/product/**").hasRole("SELLER")
                    .requestMatchers(EndpointRequest.to("health")).permitAll()
                    .anyRequest().authenticated()
                );
        return http.build();
    }
}
