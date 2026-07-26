package buy01.media.service.media;

import java.util.ArrayList;
import java.util.Collections;
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

    public List<MediaResponse> UploadPics(UploadRequest pictures) {
        if (!CanUploadToR2(pictures.getPictures())) {
            throw new MyBadRequest("One Of the images is not valid image");
        }

        if (!pictures.getType().equals("Avatar") && !pictures.getType().equals("Product")) {
            throw new MyBadRequest("The type of the media isn't valid (Avatar or Product)");
        }

        if (pictures.getType().equals("Avatar") && pictures.getPictures().length != 1) {
            throw new MyBadRequest("you should upload one avatar at time");
        }

        if (pictures.getType().equals("Product")
                && !checkRepository.existsByProductIdAndOwnerId(pictures.getProductId(), pictures.getUserId())) {
            throw new MyForbiden("The product not created or you are not the owner of the product");
        }

        try {
            List<String> urls = new ArrayList<>();
            List<MediaEntity> medias = new ArrayList<>();
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
                media = mediaRepository.save(media);
                medias.add(media);
            }
            if (pictures.getType().equals("Avatar")) {
                AcceptedUpload success = new AcceptedUpload(pictures.getUserId(), urls);
                kafkaTemplate.send("avatar.upload.success", pictures.getUserId(), success);
            } else {
                System.out.println("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
                ProductImageUploadedEvent success = new ProductImageUploadedEvent(pictures.getUserId(),
                        pictures.getProductId(), urls);
                kafkaTemplate.send("product.upload.success", pictures.getProductId(), success);
            }
            List<MediaResponse> response = medias.stream().map(m -> Mappers.mapperToMEdiaResponse(m)).toList();
            return response;
        } catch (Exception e) {
            throw new InternalError(e.getMessage());
        }
    }

    public List<MediaResponse> updateMedia(UpdateMedia request) {
        String type = request.getType();

        if (!"Avatar".equals(type) && !"Product".equals(type)) {
            throw new MyBadRequest("Media type must be Avatar or Product");
        }

        if ("Avatar".equals(type)
                && request.getNewImages() != null
                && request.getNewImages().length > 1) {
            throw new MyBadRequest("You can upload only one avatar.");
        }

        if ("Product".equals(type)
                && !checkRepository.existsByProductIdAndOwnerId(
                        request.getProductId(),
                        request.getUserId())) {
            throw new MyForbiden("Product not found or you are not the owner.");
        }

        if (request.getDeletedUrls() != null) {

            for (String url : request.getDeletedUrls()) {
                System.out.println("Deleting media with URL: " + url);
                MediaEntity media = mediaRepository.findByUrl(url);

                if (media == null) {
                    throw new MyNotFound("Media not found: " + url);
                }

                deleteMedia(media.getId());
            }
        }

        MultipartFile[] images = request.getNewImages();

        if (images != null && images.length > 0) {

            if (!CanUploadToR2(images)) {
                throw new MyBadRequest("One or more uploaded files are invalid.");
            }

            List<String> uploadedUrls = new ArrayList<>();

            try {

                for (MultipartFile image : images) {

                    String key = type + "/" + UUID.randomUUID();

                    String contentType = tika.detect(image.getBytes());

                    String url = r2.upload(
                            key,
                            contentType,
                            image.getBytes());

                    uploadedUrls.add(url);
                    // System.out.println("Uploaded media with URL: " + url);
                    MediaEntity media = new MediaEntity();
                    media.setOwnerId(request.getUserId());
                    media.setProductId(request.getProductId());
                    media.setType(type);
                    media.setUrl(url);

                    mediaRepository.save(media);
                }
                System.out.println("Uploaded media URLs: " + uploadedUrls);

                if ("Avatar".equals(type)) {

                    kafkaTemplate.send(
                            "avatar.upload.success",
                            request.getUserId(),
                            new AcceptedUpload(
                                    request.getUserId(),
                                    uploadedUrls));

                } else {

                    kafkaTemplate.send(
                            "product.upload.success",
                            request.getProductId(),
                            new ProductImageUploadedEvent(
                                    request.getUserId(),
                                    request.getProductId(),
                                    uploadedUrls));
                }

            } catch (Exception e) {
                throw new InternalError("Upload failed: " + e.getMessage());
            }
        }

        if ("Avatar".equals(type)) {

            MediaEntity avatar = mediaRepository.findByOwnerIdAndType(
                    request.getUserId(),
                    "Avatar");

            if (avatar == null) {
                return Collections.emptyList();
            }

            return List.of(Mappers.mapperToMEdiaResponse(avatar));
        }

        return mediaRepository.findByProductId(request.getProductId())
                .stream()
                .map(Mappers::mapperToMEdiaResponse)
                .toList();

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
        r2.delete(media.getUrl());
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
