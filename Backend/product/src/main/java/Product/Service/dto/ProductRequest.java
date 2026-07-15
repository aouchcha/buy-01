package Product.Service.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "Name is required") @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters") String name,

        @NotBlank(message = "description is required") @Size(min = 10, max = 1000, message = "description must be between 10 and 1000 characters") String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        @DecimalMax(value = "999999.99", message = "Price must be less than 999999.99")
        Double price,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        Integer quantity,

        List<String> imageUrls

) {
}