package service.orders.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import service.orders.models.CartItems;

@Repository
public interface CartItemsRepository extends MongoRepository<CartItems, String> {
    
    
}
