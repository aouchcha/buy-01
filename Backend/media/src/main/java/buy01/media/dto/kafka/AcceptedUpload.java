package buy01.media.dto.kafka;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AcceptedUpload {
    private final String userId;
    private final List<String> MediaUrls;

    @JsonCreator
    public AcceptedUpload(@JsonProperty("userId") String userId, @JsonProperty("MediaUrls") List<String> MediaUrls) {
        this.userId = userId;
        this.MediaUrls = MediaUrls;
    }

    public String userId() {
        return userId;
    }

    public List<String> MediaUrls() {
        return MediaUrls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcceptedUpload)) return false;
        AcceptedUpload that = (AcceptedUpload) o;
        return Objects.equals(userId, that.userId) && Objects.equals(MediaUrls, that.MediaUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, MediaUrls);
    }

    @Override
    public String toString() {
        return "AcceptedUpload[userId=" + userId + ", MediaUrls=" + MediaUrls + "]";
    }
}
