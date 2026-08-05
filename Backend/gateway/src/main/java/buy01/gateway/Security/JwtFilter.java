package buy01.gateway.Security;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import buy01.gateway.Config.Jwt;
import reactor.core.publisher.Mono;

@Component
public class JwtFilter implements GlobalFilter, Ordered {

    private final Jwt jwt;

    public JwtFilter(Jwt jwt) {
        this.jwt = jwt;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
            GatewayFilterChain chain) {

        System.out.println("========== JWT FILTER ==========");

        String path = exchange.getRequest().getPath().value();
        System.out.println("Request path: " + path);
        // Public endpoints
        if (path.startsWith("/api/auth/")) {
            System.out.println(">>>>>>>>>>>>>>> skip 0");
            return chain.filter(exchange);
        }
        
        if (HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            if (path.startsWith("/api/users") && !path.equals("/api/users/me")) {
                System.out.println(">>>>>>>>>>>>>>> skip 0");
                return chain.filter(exchange);
            }
            // Public product list
            if (path.equals("/api/product")) {
                System.out.println(">>>>>>>>>>>>>>> skip 1");
                return chain.filter(exchange);
            }

            // Public product details: /api/product/{id}
            if (path.startsWith("/api/product") && !path.equals("/api/product/myProducts")) {
                System.out.println(">>>>>>>>>>>>>>> skip 2");
                return chain.filter(exchange);
            }
        }
        // if (path.startsWith("/api/auth/")
        // || (path.startsWith("/api/product")
        // && exchange.getRequest().getMethod() == HttpMethod.GET)) {
        // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> skip");
        // return chain.filter(exchange);
        // }

        String token = resolveToken(exchange);

        if (token == null) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Missing JWT");
            return unauthorized(exchange, "Missing JWT");
        }

        if (!jwt.validateToken(token)) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Invalid JWT");
            return unauthorized(exchange, "Invalid JWT");
        }

        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< The JWT is Valid");

        String userId = jwt.getId(token);
        String role = jwt.getRole(token);

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");

                    headers.add("X-User-Id", userId);
                    headers.add("X-User-Role", role);
                })
                .build();
        return chain.filter(
                exchange.mutate()
                        .request(request)
                        .build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(message.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String resolveToken(ServerWebExchange exchange) {

        String bearer = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return null;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}