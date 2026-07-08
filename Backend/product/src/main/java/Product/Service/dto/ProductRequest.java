package Product.Service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record ProductRequest(
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "description is required")
    String description,
    
    @NotNull(message = "price is required")
    @Min(value = 0, message = "Price must be positive")
    double price,

    
    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    Integer quantity

){}