package buy01.user.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

// import com.fasterxml.jackson.databind.ObjectMapper;

import buy01.user.dto.kafka.MediaUploadEvent;

@Service
public class MediaEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(MediaEventProducer.class);

    public MediaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMediaUploadEvent(MediaUploadEvent event) {
        kafkaTemplate.send("media.upload", event.userId(), event);
        log.info("fire the upload media event");
    }
}
