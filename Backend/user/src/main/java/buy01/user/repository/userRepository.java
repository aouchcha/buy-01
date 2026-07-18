package buy01.user.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import buy01.user.model.userEntity;

@Repository
public interface userRepository extends MongoRepository<userEntity, String> {
    userEntity findByEmail(String email);
}
