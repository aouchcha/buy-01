package buy01.media.dto.kafka;


public record DeleteEvent(
   String productId,
    String userId,
    String MediaUrl
) {}