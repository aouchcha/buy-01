package buy01.media.dto.kafka;

import java.util.List;

public record AcceptedUpload(
    String userId,
    List<String> MediaUrls
) {
}