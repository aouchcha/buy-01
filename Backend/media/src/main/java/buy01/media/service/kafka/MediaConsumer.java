// package buy01.media.service.kafka;

// import org.apache.tika.Tika;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Service;

// import com.fasterxml.jackson.databind.ObjectMapper;

// import buy01.media.dto.kafka.AcceptedUpload;
// import buy01.media.dto.kafka.DeclinedUpload;
// import buy01.media.dto.kafka.MediaUploadEvent;
// import buy01.media.service.cloudflare.R2StorageService;

// @Service
// public class MediaConsumer {

//     private static final Logger log = LoggerFactory.getLogger(MediaConsumer.class);

//     private final R2StorageService r2;
//     private final Tika tika;
//     private final KafkaTemplate<String, Object> kafkaTemplate;
//     private final ObjectMapper objectMapper;

//     public MediaConsumer(R2StorageService r2StorageService, KafkaTemplate<String, Object> kafkaTemplate,
//             ObjectMapper objectMapper) {
//         this.r2 = r2StorageService;
//         this.kafkaTemplate = kafkaTemplate;
//         this.objectMapper = objectMapper;
//         this.tika = new Tika();
//     }

//     @KafkaListener(topics = "media.upload", groupId = "media-service")
//     public void consume(MediaUploadEvent event) {
//         try {
//             // MediaUploadEvent event = objectMapper.readValue(message, MediaUploadEvent.class);
//             log.info("Received media upload event for user: {}", event.userId());

//             // validate with Tika
//             String detectedType = tika.detect(event.content());
//             if (!detectedType.startsWith("image/")) {
//                 log.warn("Invalid file type {} for user {}", detectedType, event.userId());
//                 // String failed = objectMapper.writeValueAsString(
                        
//                     // );
//                 kafkaTemplate.send("media.upload.failed", event.userId(), new DeclinedUpload(event.userId(), "Invalid file type: " + detectedType));
//                 return;
//             }

//             // upload to R2
//             String url = null;
//             try {
//                 url = r2.upload(
//                         event.fileName(),
//                         detectedType,
//                         event.content());
//             } catch (Exception e) {
              
//                 DeclinedUpload    failed =  new DeclinedUpload(event.userId(), "Media couldn't be uploaded");
//                 kafkaTemplate.send("media.upload.failed", event.userId(), failed);
//                 log.error("Failed to process media upload: {}", e.getMessage());
//                 return;
//             }

//             // notify success
//             // AcceptedUpload success = new AcceptedUpload(event.userId(), url);
//             // kafkaTemplate.send("media.upload.success", event.userId(), success);
//             log.info("Avatar uploaded successfully for user: {}", event.userId());

//         } catch (Exception e) {
//             log.error("Error when try to proccess JSON in the Media Consumer");
//         } 
//     }

//     // @KafkaListener(topics = "media.delete", groupId = "media-service")
//     // public void consume()
// }
