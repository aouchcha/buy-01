package buy01.media.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import buy01.media.model.CheckEntity;

public interface CheckRepository extends MongoRepository<CheckEntity, String> {
    boolean existsByProductIdAndOwnerId(String productId, String ownerId);
    List<CheckEntity> findByProductId(String productId);
    List<CheckEntity> findByOwnerId(String ownerId);
}
