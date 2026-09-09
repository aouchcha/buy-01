package buy01.media.dto.kafka;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UserDeleted {
    private final String userId;

    @JsonCreator
    public UserDeleted(@JsonProperty("userId") String userId) {
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDeleted)) return false;
        UserDeleted that = (UserDeleted) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserDeleted[userId=" + userId + "]";
    }
}
