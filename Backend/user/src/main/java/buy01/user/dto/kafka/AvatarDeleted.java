package buy01.user.dto.kafka;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AvatarDeleted {
    private final String userId;

    @JsonCreator
    public AvatarDeleted(@JsonProperty("userId") String userId) {
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvatarDeleted)) return false;
        AvatarDeleted that = (AvatarDeleted) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "AvatarDeleted[userId=" + userId + "]";
    }
}
