package buy01.user.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.dto.kafka.AcceptedUpload;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@Service
public class CheckEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CheckEventConsumer.class);
    private final userRepository userRepository;
    private final ObjectMapper objectMapper;

    public CheckEventConsumer(userRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "media.upload.failed", groupId = "user-service")
    public void consumeFailed(String message) {
        try {
            AcceptedUpload event = objectMapper.readValue(message, AcceptedUpload.class);
            log.warn("Avatar upload failed for user: {}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process failed media upload event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "media.upload.success", groupId = "user-service")
    public void consumeSuccess(String message) {
        try {
            AcceptedUpload event = objectMapper.readValue(message, AcceptedUpload.class);
            userEntity user = userRepository.findById(event.userId()).orElseThrow(() -> new notFound("User not found"));
            user.setProfilePictureUrl(event.MediaUrl());
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to process successful media upload: {}", e.getMessage());
        }
    }
}
