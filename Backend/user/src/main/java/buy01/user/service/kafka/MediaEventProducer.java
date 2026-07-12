package buy01.user.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import buy01.user.dto.kafka.MediaUploadEvent;

@Service
public class MediaEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(MediaEventProducer.class);

    public MediaEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishMediaUploadEvent(MediaUploadEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("media.upload", event.userId(), json);
        } catch (Exception e) {
            log.error("Failed to serialize media upload event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize media upload event: " + e.getMessage(), e);
        }
    }
}
