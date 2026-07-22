package buy01.gateway.Security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // Routes publiques : pas de token requis
    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/");

    private final ReactiveJwtDecoder jwtDecoder;

    public JwtAuthFilter(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Laisser passer les routes publiques sans validation
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        // Extraire le Bearer token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // Extraire userId depuis le claim "id" (généré par User Service)
                    String userId = jwt.getClaimAsString("id");
                    // Extraire le role depuis le claim "role"
                    String role = jwt.getClaimAsString("role");

                    if (userId == null) {
                        userId = jwt.getSubject();
                    }

                    // Injecter les headers downstream
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId != null ? userId : "")
                            .header("X-User-Role", role != null ? role : "")
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(ex -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        // Priorité haute : s'exécute avant les filtres de routage
        return -100;
    }
}
