package buy01.media.dto.kafka;

public record MediaUploadEvent(
    String userId,
    String fileName,
    // String contentType,
    byte[] content
) {
}