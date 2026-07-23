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

import buy01.media.config.Exceptions.MyExeptions.MyBadRequest;
import buy01.media.config.Exceptions.MyExeptions.MyForbiden;
import buy01.media.config.Exceptions.MyExeptions.MyNotFound;
import buy01.media.dto.kafka.AcceptedUpload;
import buy01.media.dto.kafka.DeleteEvent;
import buy01.media.dto.kafka.ProductImageUploadedEvent;
import buy01.media.dto.media.UploadRequest;
import buy01.media.model.MediaEntity;
import buy01.media.repository.MediaRepository;
import buy01.media.service.cloudflare.R2StorageService;

@Service
public class MediaService {
    private final R2StorageService r2;
    private final Tika tika;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MediaRepository mediaRepository;

    public MediaService(
            R2StorageService r2,
            Tika tika,
            KafkaTemplate<String, Object> kafkaTemplate,
            MediaRepository mediaRepository
        ) {
        this.r2 = r2;
        this.tika = tika;
        this.kafkaTemplate = kafkaTemplate;
        this.mediaRepository =mediaRepository;
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
        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile pic : pictures.getPictures()) {
                String fileName = pictures.getType() + "/" + UUID.randomUUID().toString();
                String contenType = tika.detect(pic.getBytes());
                String url = r2.upload(fileName, contenType, pic.getBytes());
                System.out.println( "uuuuuuuuuuuuuuuuuuuuuuuuuuuu" + url);
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
            }else {
                System.out.println("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
                ProductImageUploadedEvent success = new ProductImageUploadedEvent(pictures.getUserId() ,pictures.getProductId(), urls);
                kafkaTemplate.send("product.upload.seccess", pictures.getProductId(), success);
            }
            return urls;
        } catch (Exception e) {
            throw new InternalError(e.getMessage());
        }
    }

    private boolean CanUploadToR2(MultipartFile[] pics) throws RuntimeException {
        for (MultipartFile pic : pics) {
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

    public String getMedia(String id) {
        MediaEntity media = mediaRepository.findById(id).orElse(null);
        if (media == null) {
            throw new MyNotFound("Media Not Found");
        }
        return media.getUrl();
    }

    public void deleteMedia(String id) {
        MediaEntity media = mediaRepository.findById(id).orElse(null);
        if (media == null) {
            throw new MyNotFound("Media Not Found");
        }
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId.equals(media.getOwnerId())) {
            throw new MyForbiden("you are not the owner of this media");
        }
        DeleteEvent event = new DeleteEvent(media.getProductId(), media.getOwnerId(), media.getUrl());
        if (media.getType().equals("Avatar")) {
            kafkaTemplate.send("avatar.deleted", media.getOwnerId(), event);
        }else {
            kafkaTemplate.send("product.deleted", media.getProductId(), event);
        }
        mediaRepository.delete(media);
    }
}
