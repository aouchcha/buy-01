package service.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
        @NotBlank(message = "fullName is required")
        @Size(min = 2, max = 100, message = "fullName must be between 2 and 100 characters") String fullName,

        @NotBlank(message = "address is required")
        @Size(min = 5, max = 255, message = "address must be between 5 and 255 characters") String address,

        @NotBlank(message = "city is required")
        @Size(min = 2, max = 100, message = "city must be between 2 and 100 characters") String city,

        @NotBlank(message = "postalCode is required")
        @Size(min = 2, max = 20, message = "postalCode must be between 2 and 20 characters") String postalCode,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+?[0-9 ()-]{8,20}$", message = "phone must be a valid phone number") String phone) {
}
