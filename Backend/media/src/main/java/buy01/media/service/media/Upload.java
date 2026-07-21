package buy01.media.service.media;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tika.Tika;
// import org.apache.tika.Tika;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import buy01.media.config.Exceptions.MyExeptions.MyBadRequest;
import buy01.media.dto.kafka.AcceptedUpload;
import buy01.media.dto.media.UploadRequest;
import buy01.media.service.cloudflare.R2StorageService;

@Service
public class Upload {
    private final R2StorageService r2;
    private final Tika tika;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Upload(
            R2StorageService r2,
            Tika tika,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.r2 = r2;
        this.tika = new Tika();
        this.kafkaTemplate = kafkaTemplate;
    }

    public void UploadPics(UploadRequest pictures) {
        if (!CanUploadToR2(pictures.getPictures())) {
            throw new MyBadRequest("One Of the images is not valid image");
        }
        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile pic : pictures.getPictures()) {
                String fileName = UUID.randomUUID().toString();
                String contenType = tika.detect(pic.getBytes());
                String url = r2.upload(fileName, contenType, pic.getBytes());
                urls.add(url);
            }
            AcceptedUpload success = new AcceptedUpload(pictures.getProductId(), pictures.getUserId(), urls);
            kafkaTemplate.send("media.upload.success", pictures.getUserId(), success);
        } catch (Exception e) {
            throw new InternalError(e.getMessage());
        }
    }

    public boolean CanUploadToR2(MultipartFile[] pics) throws RuntimeException {
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
}
