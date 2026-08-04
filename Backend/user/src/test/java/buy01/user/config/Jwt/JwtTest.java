package buy01.user.config.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTest {

    // Must be at least 32 characters long, otherwise HS256 key generation throws WeakKeyException.
    private static final String TEST_SECRET = "this-is-a-very-long-test-secret-key-1234567890";

    @Test
    void generateToken_producesNonEmptyToken() {
        Jwt jwt = new Jwt(TEST_SECRET, 60_000L);

        String token = jwt.GenerateToken("user@example.com", "ROLE_BUYER", "user-id-1");

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void validateToken_withFreshlyGeneratedToken_returnsTrue() {
        Jwt jwt = new Jwt(TEST_SECRET, 60_000L);

        String token = jwt.GenerateToken("user@example.com", "ROLE_BUYER", "user-id-1");

        assertThat(jwt.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() throws InterruptedException {
        Jwt jwt = new Jwt(TEST_SECRET, -1000L);

        String token = jwt.GenerateToken("user@example.com", "ROLE_BUYER", "user-id-1");

        assertThat(jwt.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_withNullOrEmptyToken_returnsFalse() {
        Jwt jwt = new Jwt(TEST_SECRET, 60_000L);

        assertThat(jwt.validateToken(null)).isFalse();
        assertThat(jwt.validateToken("")).isFalse();
    }

    @Test
    void getUsername_getId_getRole_returnExpectedClaims() {
        Jwt jwt = new Jwt(TEST_SECRET, 60_000L);

        String token = jwt.GenerateToken("user@example.com", "ROLE_SELLER", "user-id-42");

        assertThat(jwt.getUsername(token)).isEqualTo("user@example.com");
        assertThat(jwt.getId(token)).isEqualTo("user-id-42");
        assertThat(jwt.getRole(token)).isEqualTo("SELLER");
    }
}
