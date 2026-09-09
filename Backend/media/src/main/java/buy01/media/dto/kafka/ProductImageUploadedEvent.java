package buy01.media.dto.kafka;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProductImageUploadedEvent {
    private final String userId;
    private final String productId;
    private final List<String> imageUrls;

    @JsonCreator
    public ProductImageUploadedEvent(
            @JsonProperty("userId") String userId,
            @JsonProperty("productId") String productId,
            @JsonProperty("imageUrls") List<String> imageUrls) {
        this.userId = userId;
        this.productId = productId;
        this.imageUrls = imageUrls;
    }

    public String userId() {
        return userId;
    }

    public String productId() {
        return productId;
    }

    public List<String> imageUrls() {
        return imageUrls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductImageUploadedEvent)) return false;
        ProductImageUploadedEvent that = (ProductImageUploadedEvent) o;
        return Objects.equals(userId, that.userId) && Objects.equals(productId, that.productId)
                && Objects.equals(imageUrls, that.imageUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, productId, imageUrls);
    }

    @Override
    public String toString() {
        return "ProductImageUploadedEvent[userId=" + userId + ", productId=" + productId + ", imageUrls=" + imageUrls
                + "]";
    }
}
