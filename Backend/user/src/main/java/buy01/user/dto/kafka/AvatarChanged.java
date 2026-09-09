package buy01.user.dto.kafka;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AvatarChanged {
    private final String userId;
    private final String url;

    @JsonCreator
    public AvatarChanged(@JsonProperty("userId") String userId, @JsonProperty("url") String url) {
        this.userId = userId;
        this.url = url;
    }

    public String userId() {
        return userId;
    }

    public String url() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvatarChanged)) return false;
        AvatarChanged that = (AvatarChanged) o;
        return Objects.equals(userId, that.userId) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, url);
    }

    @Override
    public String toString() {
        return "AvatarChanged[userId=" + userId + ", url=" + url + "]";
    }
}
