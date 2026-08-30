package Product.Service.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("price")
    private double price;

    @Field("quantity")
    private Integer quantity;

    @Field("user_id")
    private String userId;

    @Builder.Default
    @Field("image_urls")
    private List<String> imageUrls = new ArrayList<>();

    @Field("category")
    private Category category;

    private LocalDateTime createdAt = LocalDateTime.now(); 
}



