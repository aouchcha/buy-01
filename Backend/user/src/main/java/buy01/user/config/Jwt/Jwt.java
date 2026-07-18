package buy01.user.config.Jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class Jwt {
    private final SecretKey secretKey;
    private final long experationTime;

    public Jwt(@Value("${jwtKey}") String Secret, @Value("${jwtExpiration}") Long experationTime) {
        this.experationTime = experationTime;
        this.secretKey = Keys.hmacShaKeyFor(Secret.getBytes(StandardCharsets.UTF_8));
    }

    public String GenerateToken(String Username, String role, String id) {
        return Jwts
                .builder()
                .subject(Username)
                .claim("role", role.replace("ROLE_", ""))
                .claim("id", id)
                .expiration(new Date(System.currentTimeMillis() + experationTime))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isEmpty())
            return false;
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(token);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public String getId(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("id", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role",
                String.class);
    }
}