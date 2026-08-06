package buy01.media.service.kafka;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import buy01.media.dto.kafka.UserDeleted;
import buy01.media.model.CheckEntity;
import buy01.media.model.MediaEntity;
import buy01.media.repository.CheckRepository;
import buy01.media.repository.MediaRepository;
import buy01.media.service.cloudflare.R2StorageService;

@Component
public class ConsumeUserEvents {
    private final MediaRepository mediaRepository;
    private final CheckRepository checkRepository;
    private final R2StorageService r2;

    public ConsumeUserEvents(
        MediaRepository mediaRepository,
        CheckRepository checkRepository,
        R2StorageService r2
    ){
        this.mediaRepository = mediaRepository;
        this.checkRepository = checkRepository;
        this.r2 = r2;
    }

    //TODO: Implement the logic
    @KafkaListener(topics = "user.deleted")
    @Transactional
    public void onUserDelet(UserDeleted event) {
        List<MediaEntity> medias = mediaRepository.findByOwnerId(event.userId());
        if (medias == null || medias.isEmpty()) {
            // throw new MyNotFound("can't found media for the user");
            return;
        }
        for (MediaEntity media : medias) {
            r2.delete(media.getUrl());
            mediaRepository.delete(media);
        } 
        List<CheckEntity> checks = checkRepository.findByOwnerId(event.userId());
        if (checks == null || checks.isEmpty()) {
            // throw new MyNotFound("can't found media for the user");
            return;
        }
        for (CheckEntity check : checks) {
            checkRepository.delete(check);
        }
    }
}
