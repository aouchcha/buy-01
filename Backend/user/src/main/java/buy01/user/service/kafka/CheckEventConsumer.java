package buy01.user.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.dto.kafka.AcceptedUpload;
import buy01.user.dto.kafka.AvatarDeleted;

import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@Service
public class CheckEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CheckEventConsumer.class);
    private final userRepository userRepository;

    public CheckEventConsumer(
            userRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "avatar.upload.success", groupId = "user-service")
    public void consumeSuccess(AcceptedUpload event) {
        try {
            userEntity user = userRepository.findById(event.userId()).orElseThrow(() -> new notFound("User not found"));
            System.out.println(event.userId());
            for (String url : event.MediaUrls()) {

                System.out.println(url);
                user.setProfilePictureUrl(url);
            }
            userRepository.save(user);
            log.info("Consume Success in user service");

        } catch (Exception e) {
            log.error("Failed to process successful media upload: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "avatar.deleted", groupId = "user-service")
    public void deleteEvent(AvatarDeleted event) {
        System.out.println("===========================================================\n avatar deleted event consumed");
        userEntity user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) {
            return;
        }
        user.setProfilePictureUrl(null);
        log.info("Consume avater deleted in user service");

        userRepository.save(user);
    }
}
