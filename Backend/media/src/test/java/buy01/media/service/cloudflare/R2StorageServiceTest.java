package buy01.media.service.cloudflare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class R2StorageServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String PUBLIC_URL = "https://cdn.example.com";

    private S3Client s3Client;
    private R2StorageService r2StorageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        r2StorageService = new R2StorageService(s3Client);

        // The `bucket` and `publicUrl` fields are populated via @Value by Spring at
        // runtime; a plain `new R2StorageService(...)` leaves them null, so we must
        // inject them manually before exercising upload()/delete().
        ReflectionTestUtils.setField(r2StorageService, "bucket", BUCKET);
        ReflectionTestUtils.setField(r2StorageService, "publicUrl", PUBLIC_URL);
    }

    @Test
    void upload_putsObjectAndReturnsPublicUrl() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn((PutObjectResponse) PutObjectResponse.builder().build());

        String fileName = "Avatar/some-uuid";
        String result = r2StorageService.upload(fileName, "image/png", "data".getBytes());

        assertThat(result).isEqualTo(PUBLIC_URL + "/" + fileName);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo(fileName);
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void delete_stripsPublicUrlPrefixAndDeletesByKey() {
        String fileName = "Avatar/some-uuid";
        String fullUrl = PUBLIC_URL + "/" + fileName;

        r2StorageService.delete(fullUrl);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo(fileName);
    }
}
