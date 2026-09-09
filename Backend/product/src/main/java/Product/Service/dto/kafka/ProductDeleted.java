package Product.Service.dto.kafka;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProductDeleted {
    private final String productId;

    @JsonCreator
    public ProductDeleted(@JsonProperty("productId") String productId) {
        this.productId = productId;
    }

    public String productId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductDeleted)) return false;
        ProductDeleted that = (ProductDeleted) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "ProductDeleted[productId=" + productId + "]";
    }
}
