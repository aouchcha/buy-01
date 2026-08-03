package buy01.media.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import buy01.media.model.MediaEntity;

public interface MediaRepository extends MongoRepository<MediaEntity, String> {
    MediaEntity findByUrl(String url);
    List<MediaEntity> findByOwnerId(String ownerId);
    List<MediaEntity> findByProductId(String productId);
    List<MediaEntity> findByProductIdAndType(String productId, String type);

    MediaEntity findByOwnerIdAndType(String ownerId, String type);
}
