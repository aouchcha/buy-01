package service.orders.repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Repository;
import service.orders.dto.BestSellingProductDTO;
import service.orders.dto.CategoryStatsDTO;
import service.orders.dto.SellerRevenueDTO;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
public class OrderStatsRepository {

    private static final String ORDERS_COLLECTION = "orders";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_STATUS = "status";
    private static final String STATUS_DELIVERED = "DELIVERED";
    private static final String FIELD_CART_ITEMS = "cart_items";
    private static final String FIELD_CART_ITEMS_QUANTITY = "cart_items.quantity";

    private final MongoTemplate mongoTemplate;

    public OrderStatsRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<BestSellingProductDTO> getBestSellingProducts(
            String sellerId, Long fromTimestamp, int limit) {

        Aggregation aggregation = newAggregation(
            match(where(FIELD_CREATED_AT).gte(fromTimestamp)),

            match(where(FIELD_STATUS).is(STATUS_DELIVERED)),

            unwind(FIELD_CART_ITEMS),

            match(where("cart_items.seller_id").is(sellerId)),

            group("cart_items.product_id")
                .first("cart_items.product_name").as("productName")
                .sum(FIELD_CART_ITEMS_QUANTITY).as("totalUnitsSold"),

            sort(DESC, "totalUnitsSold"),

            limit(limit)
        );

        return mongoTemplate.aggregate(aggregation, ORDERS_COLLECTION, BestSellingProductDTO.class)
                .getMappedResults();
    }

    public List<BestSellingProductDTO> getTopBuyedProductByUser(String userId, Long fromTimestamp, int limit) {

        Aggregation aggregation = newAggregation(
            match(where(FIELD_CREATED_AT).gte(fromTimestamp)),

            match(where(FIELD_STATUS).is(STATUS_DELIVERED)),

            unwind(FIELD_CART_ITEMS),

            match(where("user_id").is(userId)),

            group("cart_items.product_id")
                .first("cart_items.product_name").as("productName")
                .sum(FIELD_CART_ITEMS_QUANTITY).as("totalUnitsSold"),

            sort(DESC, "totalUnitsSold"),

            limit(limit)
        );

        return mongoTemplate.aggregate(aggregation, ORDERS_COLLECTION, BestSellingProductDTO.class)
                .getMappedResults();
    }

    public List<CategoryStatsDTO> getTopCategoriesByUser(String userId, Long fromTimestamp, int limit) {

        Aggregation aggregation = newAggregation(
            match(where(FIELD_CREATED_AT).gte(fromTimestamp)),

            match(where(FIELD_STATUS).is(STATUS_DELIVERED)),

            unwind(FIELD_CART_ITEMS),

            match(where("user_id").is(userId)),

            group("cart_items.category")
                .sum(FIELD_CART_ITEMS_QUANTITY).as("totalUnitsBought"),

            sort(DESC, "totalUnitsBought"),

            limit(limit)
        );

        return mongoTemplate.aggregate(aggregation, ORDERS_COLLECTION, CategoryStatsDTO.class)
                .getMappedResults();
    }

    public Double getSellerRevenue(String sellerId, Long fromTimestamp) {

        Aggregation aggregation = newAggregation(
            match(where(FIELD_CREATED_AT).gte(fromTimestamp)),

            match(where(FIELD_STATUS).is(STATUS_DELIVERED)),

            unwind(FIELD_CART_ITEMS),

            match(where("cart_items.seller_id").is(sellerId)),

            group().sum("cart_items.total_price").as("total")
        );

        SellerRevenueDTO result = mongoTemplate.aggregate(aggregation, ORDERS_COLLECTION, SellerRevenueDTO.class)
                .getUniqueMappedResult();

        return result != null ? result.getTotal() : 0.0;
    }
}