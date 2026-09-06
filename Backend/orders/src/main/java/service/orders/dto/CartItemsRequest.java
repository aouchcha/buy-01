package service.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemsRequest(
        @NotBlank(message = "productId is required") String productId,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0") Integer quantity
    ) {

}
