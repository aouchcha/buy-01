package buy01.media.model;

import org.hibernate.validator.constraints.UUID;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "media")
public class MediaEntity {
    @UUID
    private String id;

    private String ownerId;

    private String productId;

    private String type;

    private String url;
}
