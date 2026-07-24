package buy01.media.model;

import org.hibernate.validator.constraints.UUID;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "check")
public class CheckEntity {
    @UUID
    private String id;
    private String ownerId;
    private String productId;
}
