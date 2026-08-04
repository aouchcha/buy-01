package buy01.user.config.Security;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class FilterChainConfig {

    private final HeaderAuthFilter headerAuthFilter;
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
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/users/{id}").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/users/all").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/users/{id}").permitAll()
                    .requestMatchers(EndpointRequest.to("health")).permitAll()
                    .anyRequest().authenticated());
        return http.build();
    }
}
