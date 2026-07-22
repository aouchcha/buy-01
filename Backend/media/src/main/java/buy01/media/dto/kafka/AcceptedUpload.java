package buy01.media.dto.kafka;

import java.util.List;

public record AcceptedUpload(
    String productId,
    String userId,
    List<String> MediaUrls
) {
}