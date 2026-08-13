package service.orders.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import service.orders.models.Cart;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {
    
    
}
