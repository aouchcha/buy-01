package Product.Service.dto.kafka;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProductCreated {
    private final String productId;
    private final String ownerId;

    @JsonCreator
    public ProductCreated(@JsonProperty("productId") String productId, @JsonProperty("ownerId") String ownerId) {
        this.productId = productId;
        this.ownerId = ownerId;
    }

    public String productId() {
        return productId;
    }

    public String ownerId() {
        return ownerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductCreated)) return false;
        ProductCreated that = (ProductCreated) o;
        return Objects.equals(productId, that.productId) && Objects.equals(ownerId, that.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, ownerId);
    }

    @Override
    public String toString() {
        return "ProductCreated[productId=" + productId + ", ownerId=" + ownerId + "]";
    }
}
