package Product.Service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors((cors) -> cors.disable())
            .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                            HttpMethod.GET,
                            "/products/**"
                        ).permitAll()
                        .anyRequest().permitAll()/* .authenticated()*/);
            // .addFilterBefore(
            //     jwtFilter,
            //     UsernamePasswordAuthenticationFilter.class
            // );
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
