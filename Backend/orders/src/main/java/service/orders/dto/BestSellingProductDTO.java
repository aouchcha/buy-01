package service.orders.dto;


import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BestSellingProductDTO {
    @Field("_id")
    private String productId;
    private String productName;
    private Integer totalUnitsSold;
}
