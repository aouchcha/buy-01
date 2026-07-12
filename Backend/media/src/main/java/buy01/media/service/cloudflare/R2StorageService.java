package buy01.media.service.cloudflare;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class R2StorageService {

    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.public-url}")
    private String publicUrl;

    public R2StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String upload(String fileName, String contentType, byte[] data) {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(data)
        );
        return publicUrl + "/" + fileName;
    }
}