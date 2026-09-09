package service.orders.dto;

import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ShippingAddressRequest {

    @NotBlank(message = "fullName is required")
    @Size(min = 2, max = 100, message = "fullName must be between 2 and 100 characters")
    private final String fullName;

    @NotBlank(message = "address is required")
    @Size(min = 5, max = 255, message = "address must be between 5 and 255 characters")
    private final String address;

    @NotBlank(message = "city is required")
    @Size(min = 2, max = 100, message = "city must be between 2 and 100 characters")
    private final String city;

    @NotBlank(message = "postalCode is required")
    @Size(min = 2, max = 20, message = "postalCode must be between 2 and 20 characters")
    private final String postalCode;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^\\+?[0-9 ()-]{8,20}$", message = "phone must be a valid phone number")
    private final String phone;

    @JsonCreator
    public ShippingAddressRequest(
            @JsonProperty("fullName") String fullName,
            @JsonProperty("address") String address,
            @JsonProperty("city") String city,
            @JsonProperty("postalCode") String postalCode,
            @JsonProperty("phone") String phone) {
        this.fullName = fullName;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        this.phone = phone;
    }

    public String fullName() {
        return fullName;
    }

    public String address() {
        return address;
    }

    public String city() {
        return city;
    }

    public String postalCode() {
        return postalCode;
    }

    public String phone() {
        return phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShippingAddressRequest)) return false;
        ShippingAddressRequest that = (ShippingAddressRequest) o;
        return Objects.equals(fullName, that.fullName) && Objects.equals(address, that.address)
                && Objects.equals(city, that.city) && Objects.equals(postalCode, that.postalCode)
                && Objects.equals(phone, that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, address, city, postalCode, phone);
    }

    @Override
    public String toString() {
        return "ShippingAddressRequest[fullName=" + fullName + ", address=" + address + ", city=" + city
                + ", postalCode=" + postalCode + ", phone=" + phone + "]";
    }
}
