package buy01.media.Helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import buy01.media.dto.media.MediaResponse;
import buy01.media.model.MediaEntity;

class MappersTest {

    @Test
    void mapperToMEdiaResponse_convertsAllFieldsCorrectly() {
        MediaEntity entity = new MediaEntity();
        entity.setId("media-id-1");
        entity.setUrl("https://cdn.example.com/Avatar/abc");
        entity.setProductId("product-id-1");
        entity.setOwnerId("owner-id-1");
        entity.setType("Product");

        MediaResponse response = Mappers.mapperToMEdiaResponse(entity);

        assertThat(response.getId()).isEqualTo("media-id-1");
        assertThat(response.getUrl()).isEqualTo("https://cdn.example.com/Avatar/abc");
        assertThat(response.getProductId()).isEqualTo("product-id-1");
    }
}
