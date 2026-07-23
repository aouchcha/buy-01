package buy01.user.dto.kafka;

public record DeleteEvent(
   String productId,
    String userId,
    String MediaUrl
) {}
