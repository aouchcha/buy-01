package service.orders.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import service.orders.models.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByIdAndUserId(String id, String userId);

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
   
}