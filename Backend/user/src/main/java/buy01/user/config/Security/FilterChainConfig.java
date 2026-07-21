package buy01.user.config.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class FilterChainConfig {
    // private final RateLimiterFilter rateLimitFilter;
    // private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    // private final CustomAccessDeniedHandler accessDeniedHandler;

    // public FilterChainConfig(JwtFilter jwtFilter, RateLimiterFilter rateLimitFilter, CustomAuthenticationEntryPoint authenticationEntryPoint, CustomAccessDeniedHandler accessDeniedHandler) {
    //     this.jwtFilter = jwtFilter;
    //     this.rateLimitFilter = rateLimitFilter;
    //     this.authenticationEntryPoint = authenticationEntryPoint;
    //     this.accessDeniedHandler = accessDeniedHandler;
    // }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable())
                .cors((cors) -> cors.disable())
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((request) -> request
                        .anyRequest().permitAll());
        return http.build();
    }
}
