package buy01.user.dto.kafka;

public record MediaUploadEvent(
    String userId,
    String fileName,
    // String contentType,
    byte[] content
) {}
