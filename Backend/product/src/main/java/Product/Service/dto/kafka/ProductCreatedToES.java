package Product.Service.dto.kafka;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProductCreatedToES {
    private final String productId;
    private final String name;
    private final String description;
    private final double price;
    private final Integer quantity;
    private final String userId;
    private final String category;
    private final List<String> imageUrls;

    @JsonCreator
    public ProductCreatedToES(
            @JsonProperty("productId") String productId,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("price") double price,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("userId") String userId,
            @JsonProperty("category") String category,
            @JsonProperty("imageUrls") List<String> imageUrls) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.userId = userId;
        this.category = category;
        this.imageUrls = imageUrls;
    }

    public String productId() {
        return productId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public double price() {
        return price;
    }

    public Integer quantity() {
        return quantity;
    }

    public String userId() {
        return userId;
    }

    public String category() {
        return category;
    }

    public List<String> imageUrls() {
        return imageUrls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductCreatedToES)) return false;
        ProductCreatedToES that = (ProductCreatedToES) o;
        return Double.compare(price, that.price) == 0 && Objects.equals(productId, that.productId)
                && Objects.equals(name, that.name) && Objects.equals(description, that.description)
                && Objects.equals(quantity, that.quantity) && Objects.equals(userId, that.userId)
                && Objects.equals(category, that.category) && Objects.equals(imageUrls, that.imageUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, name, description, price, quantity, userId, category, imageUrls);
    }

    @Override
    public String toString() {
        return "ProductCreatedToES[productId=" + productId + ", name=" + name + ", description=" + description
                + ", price=" + price + ", quantity=" + quantity + ", userId=" + userId + ", category=" + category
                + ", imageUrls=" + imageUrls + "]";
    }
}
