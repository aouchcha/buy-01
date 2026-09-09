package Product.Service.dto;

import java.util.Objects;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import Product.Service.model.Category;

public final class ProductRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
    private final String name;

    @NotBlank(message = "description is required")
    @Size(min = 10, max = 1000, message = "description must be between 10 and 1000 characters")
    private final String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price must be less than 999999.99")
    private final Double price;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private final Integer quantity;

    @NotNull(message = "category is required")
    private final Category category;

    @JsonCreator
    public ProductRequest(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("price") Double price,
            @JsonProperty("quantity") Integer quantity,
            @JsonProperty("category") Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Double price() {
        return price;
    }

    public Integer quantity() {
        return quantity;
    }

    public Category category() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRequest)) return false;
        ProductRequest that = (ProductRequest) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description)
                && Objects.equals(price, that.price) && Objects.equals(quantity, that.quantity)
                && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, price, quantity, category);
    }

    @Override
    public String toString() {
        return "ProductRequest[name=" + name + ", description=" + description + ", price=" + price + ", quantity="
                + quantity + ", category=" + category + "]";
    }
}
