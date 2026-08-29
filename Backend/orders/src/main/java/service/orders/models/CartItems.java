package service.orders.models;



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
public class CartItems {
    @Id
    private String id;

    @Field("seller_id")
    private String sellerId;

    @Field("product_id")
    private String productId;

    @Field("product_name")
    private String productName;

    @Field("price")
    private Double price;

    @Field("quantity")
    private Integer quantity;

    @Field("total_price")
    private Double totalPrice;

    @Field("OutOfStock")
    private Boolean OutOfStock;
    
}
