package service.orders.models;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("status")
    private OrderStatus status;

    @Field("total_Amount")
    private Double totalAmount;

    @Field("full_name")
    private String fullName;

    @Field("address")
    private String address;

    @Field("City")
    private String city;

    @Field("postal_code")
    private String postalCode;

    @Field("phone_number")
    private String phoneNumber;

    @Field("created_at")
    private Long createdAt;

    @Field("cart_items")
    private List<CartItems> cartItems;

    @Field("payment_method")
    private PaymentMethod paymentMethod;
    


}