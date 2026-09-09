package buy01.media.dto.kafka;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DeleteEvent {
    private final String productId;
    private final String userId;
    private final String MediaUrl;

    @JsonCreator
    public DeleteEvent(
            @JsonProperty("productId") String productId,
            @JsonProperty("userId") String userId,
            @JsonProperty("MediaUrl") String MediaUrl) {
        this.productId = productId;
        this.userId = userId;
        this.MediaUrl = MediaUrl;
    }

    public String productId() {
        return productId;
    }

    public String userId() {
        return userId;
    }

    public String MediaUrl() {
        return MediaUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteEvent)) return false;
        DeleteEvent that = (DeleteEvent) o;
        return Objects.equals(productId, that.productId) && Objects.equals(userId, that.userId)
                && Objects.equals(MediaUrl, that.MediaUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, userId, MediaUrl);
    }

    @Override
    public String toString() {
        return "DeleteEvent[productId=" + productId + ", userId=" + userId + ", MediaUrl=" + MediaUrl + "]";
    }
}
