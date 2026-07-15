package Product.Service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import Product.Service.model.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByIdAndUserId(String id, String userId);

    List<Product> findByUserId(String id);
}
