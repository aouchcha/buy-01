package buy01.user.dto.kafka;

import java.util.List;

public record AcceptedUpload(
    String productId,
    String userId,
    List<String> MediaUrls
) {
}
