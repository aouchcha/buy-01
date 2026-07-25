package buy01.media.Helpers;

import buy01.media.dto.media.MediaResponse;
import buy01.media.model.MediaEntity;

public class Mappers {
    public static MediaResponse mapperToMEdiaResponse(MediaEntity media) {
        MediaResponse dto = new MediaResponse();
        dto.setId(media.getId());
        dto.setUrl(media.getUrl());
        dto.setProductId(media.getProductId());
        return dto;
    }
}
