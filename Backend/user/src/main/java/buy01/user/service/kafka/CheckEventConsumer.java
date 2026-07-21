package buy01.user.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.dto.kafka.AcceptedUpload;
import buy01.user.dto.kafka.DeclinedUpload;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@Service
public class CheckEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CheckEventConsumer.class);
    private final userRepository userRepository;
    // private final ObjectMapper objectMapper;

    public CheckEventConsumer(
        userRepository userRepository
        // , ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        // this.objectMapper = objectMapper;
    }

    // @KafkaListener(topics = "media.upload.failed", groupId = "user-service")
    // public void consumeFailed(DeclinedUpload event) {
    //     try {
    //         // DeclinedUpload event = objectMapper.readValue(message, DeclinedUpload.class);
    //         userEntity user = userRepository.findById(event.userId()).orElseThrow(() -> new notFound("User not found"));
    //         log.info("Declined Upload in user service");
    //         // userRepository.delete(user);
    //     } catch (Exception e) {
    //         log.error("Failed to process failed media upload: {}", e.getMessage());
    //     }
    // }

    @KafkaListener(topics = "media.upload.success", groupId = "user-service")
    public void consumeSuccess(AcceptedUpload event) {
        try {
            // AcceptedUpload event = objectMapper.readValue(message, AcceptedUpload.class);
            userEntity user = userRepository.findById(event.userId()).orElseThrow(() -> new notFound("User not found"));
            user.setProfilePictureUrl(event.MediaUrl());
            userRepository.save(user);
            log.info("Consume Success in user service");

        } catch (Exception e) {
            log.error("Failed to process successful media upload: {}", e.getMessage());
        }
    }
}
