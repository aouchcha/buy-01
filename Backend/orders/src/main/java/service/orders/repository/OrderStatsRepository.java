package service.orders.repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Repository;
import service.orders.dto.BestSellingProductDTO;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
public class OrderStatsRepository {

    private final MongoTemplate mongoTemplate;

    public OrderStatsRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<BestSellingProductDTO> getBestSellingProducts(
            String sellerId, Long fromTimestamp, int limit) {

        Aggregation aggregation = newAggregation(
            match(where("created_at").gte(fromTimestamp)),

            match(where("status").is("PENDING")),

            unwind("cart_items"),

            match(where("cart_items.seller_id").is(sellerId)),

            group("cart_items.product_id")
                .first("cart_items.product_name").as("productName")
                .sum("cart_items.quantity").as("totalUnitsSold"),

            sort(DESC, "totalUnitsSold"),

            limit(limit)
        );

        return mongoTemplate.aggregate(aggregation, "orders", BestSellingProductDTO.class)
                .getMappedResults();
    }

    public List<BestSellingProductDTO> getTopBuyedProductByUser(String userId, Long fromTimestamp, int limit) {

        Aggregation aggregation = newAggregation(
            match(where("created_at").gte(fromTimestamp)),

            match(where("status").is("DELIVERED")),

            unwind("cart_items"),
            
            match(where("user_id").is(userId)),

            group("cart_items.product_id")
                .first("cart_items.product_name").as("productName")
                .sum("cart_items.quantity").as("totalUnitsBuyed"),

            sort(DESC, "totalUnitsBuyed"),

            limit(limit)
        );

        return mongoTemplate.aggregate(aggregation, "orders", BestSellingProductDTO.class)
                .getMappedResults();
    }
}