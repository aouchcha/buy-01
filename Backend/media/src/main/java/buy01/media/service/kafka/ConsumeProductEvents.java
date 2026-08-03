package buy01.media.service.kafka;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import buy01.media.config.Exceptions.MyExeptions.MyNotFound;
import buy01.media.dto.kafka.ProductCreated;
import buy01.media.dto.kafka.ProductDeleted;
import buy01.media.model.CheckEntity;
import buy01.media.model.MediaEntity;
import buy01.media.repository.CheckRepository;
import buy01.media.repository.MediaRepository;
import buy01.media.service.cloudflare.R2StorageService;

@Component
public class ConsumeProductEvents {
    private final CheckRepository checkRepository;
    private final MediaRepository mediaRepository;
    private final R2StorageService r2;

    public ConsumeProductEvents(
        CheckRepository checkRepository,
        MediaRepository mediaRepository,
        R2StorageService r2
    ) {
        this.checkRepository = checkRepository;
        this.mediaRepository = mediaRepository;
        this.r2 = r2; 
    }

    @KafkaListener(topics = "product.created")
    public void onProductCreation(ProductCreated event) {
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        CheckEntity check = new CheckEntity();
        check.setOwnerId(event.ownerId());
        check.setProductId(event.productId());
        checkRepository.save(check);
    }

    //TODO: Create the Logic
    @KafkaListener(topics = "product.deleted")
    public void onProductDeletion(ProductDeleted event) {
        List<MediaEntity> medias = mediaRepository.findByProductId(event.productId());
        if (medias == null || medias.isEmpty()) {
            // throw new MyNotFound("can't found media for the user");
            return;
        }
        for (MediaEntity media : medias) {
            r2.delete(media.getUrl());
            mediaRepository.delete(media);
        } 
        List<CheckEntity> checks = checkRepository.findByProductId(event.productId());
        if (checks == null || checks.isEmpty()) {
            // throw new MyNotFound("can't found media for the user");
            return;
        }
        for (CheckEntity check : checks) {
            checkRepository.delete(check);
        }
    }
}
