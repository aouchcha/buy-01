package buy01.media.service.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.tika.Tika;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import buy01.media.config.Exceptions.MyExeptions.MyBadRequest;
import buy01.media.config.Exceptions.MyExeptions.MyForbiden;
import buy01.media.config.Exceptions.MyExeptions.MyNotFound;
import buy01.media.dto.media.MediaResponse;
import buy01.media.dto.media.UploadRequest;
import buy01.media.model.MediaEntity;
import buy01.media.repository.CheckRepository;
import buy01.media.repository.MediaRepository;
import buy01.media.service.cloudflare.R2StorageService;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    private static final String USER_ID = "user-id-1";
    private static final String PRODUCT_ID = "product-id-1";

    private R2StorageService r2;
    private Tika tika;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private MediaRepository mediaRepository;
    private CheckRepository checkRepository;
    private MediaService mediaService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        r2 = mock(R2StorageService.class);
        tika = mock(Tika.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        mediaRepository = mock(MediaRepository.class);
        checkRepository = mock(CheckRepository.class);
        mediaService = new MediaService(r2, tika, kafkaTemplate, mediaRepository, checkRepository);

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(USER_ID);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MultipartFile buildImageFile() {
        return new MockMultipartFile("file", "avatar.png", "image/png", "fake-image-bytes".getBytes());
    }

    // ---------- UploadPics ----------

    @Test
    void uploadPics_avatarNominal_returnsMediaResponseAndPublishesEvent() {
        when(tika.detect(any(byte[].class))).thenReturn("image/png");
        when(r2.upload(anyString(), anyString(), any(byte[].class))).thenReturn("https://cdn.example.com/Avatar/abc");
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(invocation -> {
            MediaEntity entity = invocation.getArgument(0);
            entity.setId("media-id-1");
            return entity;
        });

        UploadRequest request = new UploadRequest();
        request.setUserId(USER_ID);
        request.setType("Avatar");
        request.setPictures(new MultipartFile[] { buildImageFile() });

        List<MediaResponse> response = mediaService.UploadPics(request);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUrl()).isEqualTo("https://cdn.example.com/Avatar/abc");
        verify(kafkaTemplate, times(1)).send(eq("avatar.upload.success"), eq(USER_ID), any());
    }

    @Test
    void uploadPics_productNominal_returnsMediaResponseAndPublishesEvent() {
        when(tika.detect(any(byte[].class))).thenReturn("image/png");
        when(checkRepository.existsByProductIdAndOwnerId(PRODUCT_ID, USER_ID)).thenReturn(true);
        when(r2.upload(anyString(), anyString(), any(byte[].class))).thenReturn("https://cdn.example.com/Product/abc");
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(invocation -> {
            MediaEntity entity = invocation.getArgument(0);
            entity.setId("media-id-2");
            return entity;
        });

        UploadRequest request = new UploadRequest();
        request.setUserId(USER_ID);
        request.setProductId(PRODUCT_ID);
        request.setType("Product");
        request.setPictures(new MultipartFile[] { buildImageFile() });

        List<MediaResponse> response = mediaService.UploadPics(request);

        assertThat(response).hasSize(1);
        verify(kafkaTemplate, times(1)).send(eq("product.upload.success"), eq(PRODUCT_ID), any());
    }

    @Test
    void uploadPics_withNonImageContent_throwsMyBadRequest() {
        when(tika.detect(any(byte[].class))).thenReturn("application/pdf");

        UploadRequest request = new UploadRequest();
        request.setUserId(USER_ID);
        request.setType("Avatar");
        request.setPictures(new MultipartFile[] { buildImageFile() });

        assertThatThrownBy(() -> mediaService.UploadPics(request))
                .isInstanceOf(MyBadRequest.class);
    }

    @Test
    void uploadPics_withInvalidType_throwsMyBadRequest() {
        when(tika.detect(any(byte[].class))).thenReturn("image/png");

        UploadRequest request = new UploadRequest();
        request.setUserId(USER_ID);
        request.setType("Invalid");
        request.setPictures(new MultipartFile[] { buildImageFile() });

        assertThatThrownBy(() -> mediaService.UploadPics(request))
                .isInstanceOf(MyBadRequest.class);
    }

    @Test
    void uploadPics_avatarWithMoreThanOnePicture_throwsMyBadRequest() {
        when(tika.detect(any(byte[].class))).thenReturn("image/png");

        UploadRequest request = new UploadRequest();
        request.setUserId(USER_ID);
        request.setType("Avatar");
        request.setPictures(new MultipartFile[] { buildImageFile(), buildImageFile() });

        assertThatThrownBy(() -> mediaService.UploadPics(request))
                .isInstanceOf(MyBadRequest.class);
    }

    @Test
    void uploadPics_productNotOwned_throwsMyForbiden() {
        when(tika.detect(any(byte[].class))).thenReturn("image/png");
        when(checkRepository.existsByProductIdAndOwnerId(PRODUCT_ID, USER_ID)).thenReturn(false);

        UploadRequest request = new UploadRequest();
        request.setUserId(USER_ID);
        request.setProductId(PRODUCT_ID);
        request.setType("Product");
        request.setPictures(new MultipartFile[] { buildImageFile() });

        assertThatThrownBy(() -> mediaService.UploadPics(request))
                .isInstanceOf(MyForbiden.class);
    }

    @Test
    void uploadPics_whenR2UploadFailsUnexpectedly_wrapsInInternalError() {
        when(tika.detect(any(byte[].class))).thenReturn("image/png");
        when(r2.upload(anyString(), anyString(), any(byte[].class)))
                .thenThrow(new RuntimeException("R2 unreachable"));

        UploadRequest request = new UploadRequest();
        request.setUserId(USER_ID);
        request.setType("Avatar");
        request.setPictures(new MultipartFile[] { buildImageFile() });

        // The real service's try/catch around the upload loop wraps any generic
        // exception into java.lang.InternalError (note: the code does not import
        // the custom buy01.media...MyExeptions.InternalError, so it resolves to
        // the JDK's java.lang.InternalError).
        assertThatThrownBy(() -> mediaService.UploadPics(request))
                .isInstanceOf(InternalError.class);

        verify(mediaRepository, never()).save(any(MediaEntity.class));
    }

    // ---------- getMedia ----------

    @Test
    void getMedia_whenFound_returnsMediaResponse() {
        MediaEntity media = new MediaEntity();
        media.setId("media-id-1");
        media.setUrl("https://cdn.example.com/Avatar/abc");
        media.setProductId(null);
        when(mediaRepository.findById("media-id-1")).thenReturn(Optional.of(media));

        MediaResponse response = mediaService.getMedia("media-id-1");

        assertThat(response.getId()).isEqualTo("media-id-1");
        assertThat(response.getUrl()).isEqualTo("https://cdn.example.com/Avatar/abc");
    }

    @Test
    void getMedia_whenNotFound_throwsMyNotFound() {
        when(mediaRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.getMedia("missing-id"))
                .isInstanceOf(MyNotFound.class);
    }

    // ---------- deleteMedia ----------

    @Test
    void deleteMedia_whenOwnerAndAvatar_deletesAndPublishesAvatarDeletedEvent() {
        MediaEntity media = new MediaEntity();
        media.setId("media-id-1");
        media.setOwnerId(USER_ID);
        media.setType("Avatar");
        media.setUrl("https://cdn.example.com/Avatar/abc");
        when(mediaRepository.findById("media-id-1")).thenReturn(Optional.of(media));

        mediaService.deleteMedia("media-id-1");

        verify(r2, times(1)).delete("https://cdn.example.com/Avatar/abc");
        verify(mediaRepository, times(1)).delete(media);
        verify(kafkaTemplate, times(1)).send(eq("avatar.deleted"), eq(USER_ID), any());
    }

    @Test
    void deleteMedia_whenOwnerAndProduct_deletesAndPublishesProductMediaDeletedEvent() {
        MediaEntity media = new MediaEntity();
        media.setId("media-id-2");
        media.setOwnerId(USER_ID);
        media.setProductId(PRODUCT_ID);
        media.setType("Product");
        media.setUrl("https://cdn.example.com/Product/abc");
        when(mediaRepository.findById("media-id-2")).thenReturn(Optional.of(media));

        mediaService.deleteMedia("media-id-2");

        verify(kafkaTemplate, times(1)).send(eq("product.media.deleted"), eq(PRODUCT_ID), any());
    }

    @Test
    void deleteMedia_whenNotFound_throwsMyNotFound() {
        when(mediaRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.deleteMedia("missing-id"))
                .isInstanceOf(MyNotFound.class);
    }

    @Test
    void deleteMedia_whenNotOwner_throwsMyForbiden() {
        MediaEntity media = new MediaEntity();
        media.setId("media-id-3");
        media.setOwnerId("someone-else");
        media.setType("Avatar");
        media.setUrl("https://cdn.example.com/Avatar/xyz");
        when(mediaRepository.findById("media-id-3")).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> mediaService.deleteMedia("media-id-3"))
                .isInstanceOf(MyForbiden.class);

        verify(r2, never()).delete(anyString());
        verify(mediaRepository, never()).delete(any(MediaEntity.class));
    }

    // ---------- getAll ----------

    @Test
    void getAll_returnsMediaOwnedByCurrentUser() {
        MediaEntity media = new MediaEntity();
        media.setId("media-id-1");
        media.setOwnerId(USER_ID);
        media.setUrl("https://cdn.example.com/Avatar/abc");
        when(mediaRepository.findByOwnerId(USER_ID)).thenReturn(List.of(media));

        List<MediaResponse> response = mediaService.getAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo("media-id-1");
    }
}
