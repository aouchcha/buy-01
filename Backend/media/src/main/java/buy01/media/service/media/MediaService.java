package buy01.media.service.media;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import buy01.media.Helpers.Mappers;
import buy01.media.config.Exceptions.MyExeptions.MyBadRequest;
import buy01.media.config.Exceptions.MyExeptions.MyForbiden;
import buy01.media.config.Exceptions.MyExeptions.MyNotFound;
import buy01.media.dto.kafka.AcceptedUpload;
import buy01.media.dto.kafka.AvatarDeleted;
import buy01.media.dto.kafka.DeleteEvent;
import buy01.media.dto.kafka.ProductImageUploadedEvent;
import buy01.media.dto.media.MediaResponse;
import buy01.media.dto.media.UpdateMedia;
import buy01.media.dto.media.UploadRequest;
import buy01.media.model.CheckEntity;
import buy01.media.model.MediaEntity;
import buy01.media.repository.CheckRepository;
import buy01.media.repository.MediaRepository;
import buy01.media.service.cloudflare.R2StorageService;

@Service
public class MediaService {
    private final R2StorageService r2;
    private final Tika tika;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MediaRepository mediaRepository;
    private final CheckRepository checkRepository;

    public MediaService(
            R2StorageService r2,
            Tika tika,
            KafkaTemplate<String, Object> kafkaTemplate,
            MediaRepository mediaRepository,
            CheckRepository checkRepository) {
        this.r2 = r2;
        this.tika = tika;
        this.kafkaTemplate = kafkaTemplate;
        this.mediaRepository = mediaRepository;
        this.checkRepository = checkRepository;
    }

    public List<String> UploadPics(UploadRequest pictures) {
        if (!CanUploadToR2(pictures.getPictures())) {
            throw new MyBadRequest("One Of the images is not valid image");
        }

        if (!pictures.getType().equals("Avatar") && !pictures.getType().equals("Product")) {
            throw new MyBadRequest("The type of the media isn't valid (Avatar or Product)");
        }

        if (pictures.getType().equals("Avatar") && pictures.getPictures().length != 1) {
            throw new MyBadRequest("you should upload one avatar at time");
        }

        if (pictures.getType().equals("Procuct")
                && !checkRepository.existsByProductIdAndOwnerId(pictures.getProductId(), pictures.getUserId())) {
            throw new MyForbiden("The product not created or you are not the owner of the product");
        }

        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile pic : pictures.getPictures()) {
                String fileName = pictures.getType() + "/" + UUID.randomUUID().toString();
                String contenType = tika.detect(pic.getBytes());
                String url = r2.upload(fileName, contenType, pic.getBytes());
                System.out.println("uuuuuuuuuuuuuuuuuuuuuuuuuuuu" + url);
                urls.add(url);
                MediaEntity media = new MediaEntity();
                media.setOwnerId(pictures.getUserId());
                media.setProductId(pictures.getProductId());
                media.setType(pictures.getType());
                media.setUrl(url);
                mediaRepository.save(media);
            }
            if (pictures.getType().equals("Avatar")) {
                AcceptedUpload success = new AcceptedUpload(pictures.getUserId(), urls);
                kafkaTemplate.send("avatar.upload.success", pictures.getUserId(), success);
            } else {
                System.out.println("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
                ProductImageUploadedEvent success = new ProductImageUploadedEvent(pictures.getUserId(),
                        pictures.getProductId(), urls);
                kafkaTemplate.send("product.upload.seccess", pictures.getProductId(), success);
            }
            return urls;
        } catch (Exception e) {
            throw new InternalError(e.getMessage());
        }
    }

    public List<MediaResponse> updateMedia(UpdateMedia request) {
        String[] toRemove = request.getDeletedUrls();
        System.out.println("NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN" + toRemove.length);
        for (String url : toRemove) {
            System.out.println("UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU" + url);
            MediaEntity media = mediaRepository.findByUrl(url);
            if (media == null) {
                throw new MyNotFound("Media not found");
            }
            deleteMedia(media.getId());
        }

        if (!CanUploadToR2(request.getNewImages())) {
            System.out.println("KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKk" + request.getNewImages().length);

            throw new MyBadRequest("One Of the images is not valid image");
        }

        if (!request.getType().equals("Avatar") && !request.getType().equals("Product")) {
            throw new MyBadRequest("The type of the media isn't valid (Avatar or Product)");
        }

        if (request.getType().equals("Avatar") && request.getNewImages().length != 1) {
            throw new MyBadRequest("you should upload one avatar at time");
        }

        if (request.getType().equals("Procuct")
                && !checkRepository.existsByProductIdAndOwnerId(request.getProductId(), request.getUserId())) {
            throw new MyForbiden("The product not created or you are not the owner of the product");
        }

        try {
            List<String> urls = new ArrayList<>();
            List<MediaEntity> medias = new ArrayList<>();
            for (MultipartFile pic : request.getNewImages()) {
                String fileName = request.getType() + "/" + UUID.randomUUID().toString();
                String contenType = tika.detect(pic.getBytes());
                String url = r2.upload(fileName, contenType, pic.getBytes());
                System.out.println("uuuuuuuuuuuuuuuuuuuuuuuuuuuu" + url);
                urls.add(url);
                MediaEntity media = new MediaEntity();
                media.setOwnerId(request.getUserId());
                media.setProductId(request.getProductId());
                media.setType(request.getType());
                media.setUrl(url);
                media = mediaRepository.save(media);
                medias.add(media);
            }
            if (request.getType().equals("Avatar")) {
                AcceptedUpload success = new AcceptedUpload(request.getUserId(), urls);
                kafkaTemplate.send("avatar.upload.success", request.getUserId(), success);
            } else {
                System.out.println("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
                ProductImageUploadedEvent success = new ProductImageUploadedEvent(request.getUserId(),
                        request.getProductId(), urls);
                kafkaTemplate.send("product.upload.seccess", request.getProductId(), success);
            }
            List<MediaResponse> response = medias.stream().map(m -> Mappers.mapperToMEdiaResponse(m)).toList(); 
            return response;
        } catch (Exception e) {
            throw new InternalError(e.getMessage());
        }
    }

    private boolean CanUploadToR2(MultipartFile[] pics) throws RuntimeException {
        for (MultipartFile pic : pics) {
            System.out.println("KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKk" + pic.getName());

            try {
                String detectedType = tika.detect(pic.getBytes());
                if (!detectedType.startsWith("image/")) {
                    return false;
                }
            } catch (Exception e) {
                throw new InternalError(e.getMessage());
            }
        }
        return true;
    }

    public List<MediaResponse> getAll() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        final List<MediaEntity> medias = mediaRepository.findByOwnerId(userId);
        if (medias == null) {
            return new ArrayList<>();
        }
        List<MediaResponse> urls = medias.stream().map(m -> Mappers.mapperToMEdiaResponse(m)).toList();
        return urls;
    }

    public MediaResponse getMedia(String id) {
        MediaEntity media = mediaRepository.findById(id).orElse(null);
        if (media == null) {
            throw new MyNotFound("Media Not Found");
        }
        return Mappers.mapperToMEdiaResponse(media);
    }

    public void deleteMedia(String id) {
        MediaEntity media = mediaRepository.findById(id).orElse(null);
        if (media == null) {
            throw new MyNotFound("Media Not Found");
        }
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!userId.equals(media.getOwnerId())) {
            throw new MyForbiden("you are not the owner of this media");
        }
        if (media.getType().equals("Avatar")) {
            AvatarDeleted event = new AvatarDeleted(userId);
            kafkaTemplate.send("avatar.deleted", media.getOwnerId(), event);
        } else {
            DeleteEvent event = new DeleteEvent(media.getProductId(), media.getOwnerId(), media.getUrl());
            kafkaTemplate.send("product.media.deleted", media.getProductId(), event);
        }
        mediaRepository.delete(media);
    }

}
