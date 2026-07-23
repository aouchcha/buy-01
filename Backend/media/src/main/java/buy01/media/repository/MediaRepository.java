package buy01.media.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import buy01.media.model.MediaEntity;

public interface MediaRepository extends MongoRepository<MediaEntity, String> {
    String findByUrl(String url);
}
