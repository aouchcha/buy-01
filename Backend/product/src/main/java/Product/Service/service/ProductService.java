package Product.Service.service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import Product.Service.dto.ProductRequest;
import Product.Service.dto.ProductResponse;
import Product.Service.dto.kafka.ProductCreated;
import Product.Service.dto.kafka.ProductDeleted;
import Product.Service.exception.ForbiddenException;
import Product.Service.exception.ProductNotFoundException;
import Product.Service.model.Category;
import Product.Service.model.Product;
import Product.Service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import Product.Service.dto.StockUpdateResult;
import Product.Service.dto.StockRequest;
import Product.Service.dto.ItemStockStatus;
import java.util.ArrayList;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ProductService {
    private static final String PRODUCT_NOT_FOUND = "Product not found";

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    private final KafkaTemplate<String, Object> kafka;

    public ProductResponse getProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        return toResponse(product);
    }

    public List<ProductResponse> getAllProduct() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> searchProducts(
            String keyword, Category category, Double minPrice, Double maxPrice,
            String sortBy, int page, int size) {
        Query query = new Query();

        if (keyword != null && !keyword.isBlank()) {
            String pattern = Pattern.quote(keyword.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern, "i"),
                    Criteria.where("description").regex(pattern, "i")));
        }

        if (category != null) {
            query.addCriteria(Criteria.where("category").is(category));
        }

        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = Criteria.where("price");
            if (minPrice != null) {
                priceCriteria = priceCriteria.gte(minPrice);
            }
            if (maxPrice != null) {
                priceCriteria = priceCriteria.lte(maxPrice);
            }
            query.addCriteria(priceCriteria);
        }

        Sort sort;
        if ("price_desc".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("newest".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            sort = Sort.by(Sort.Direction.ASC, "price");
        }

        query.with(sort).with(PageRequest.of(page, size));

        return mongoTemplate.find(query, Product.class).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse createProduct(ProductRequest productRequest, String userId) {
        Product product = Product.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .quantity(productRequest.quantity())
                .category(productRequest.category())
                .userId(userId)
                .build();
        product = productRepository.save(product);
        ProductCreated event = new ProductCreated(product.getId(), userId);
        kafka.send("product.created", userId, event);
        return toResponse(product);
    }

    public ProductResponse updateProduct(ProductRequest productRequest, String id, String userId) {
        Product product = ownedProductOrThrow(id, userId);

        product.setName(productRequest.name());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());
        product.setQuantity(productRequest.quantity());
        product.setCategory(productRequest.category());
        product = productRepository.save(product);
        return toResponse(product);
    }

    public void deleteProduct(String id, String userId) {
        ownedProductOrThrow(id, userId);
        ProductDeleted event = new ProductDeleted(id);
        kafka.send("product.deleted", id, event);
        productRepository.deleteById(id);
    }

    public Product addImageUrl(String productId, List<String> imageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        product.setImageUrls(imageUrls);
        return productRepository.save(product);
    }

    public void removeImageUrl(String productId, String url) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        List<String> urls = product.getImageUrls();
        urls.remove(url);
        product.setImageUrls(urls);
        productRepository.save(product);
    }


    public List<ProductResponse> getMyProduct(String userId) {
        return productRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Product ownedProductOrThrow(String id, String userId) {
        return productRepository.findByIdAndUserId(id, userId)
                .orElseGet(() -> {
                    if (productRepository.existsById(id)) {
                        throw new ForbiddenException("You do not own this product");
                    }
                    throw new ProductNotFoundException(PRODUCT_NOT_FOUND);
                });
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId(), product.getCategory(), product.getImageUrls());
    }

    public StockUpdateResult updateStock(List<StockRequest> StockRequest) {

        List<ItemStockStatus> items = new ArrayList<>();

        for (StockRequest request : StockRequest) {

            Optional<Product> optionalProduct = productRepository.findById(request.productId());

            if (optionalProduct.isEmpty()) {
                items.add(new ItemStockStatus(
                        request.productId(),
                        false,
                        request.quantity(),
                        0,
                        PRODUCT_NOT_FOUND));

                continue;
            }

            Product product = optionalProduct.get();

            if (product.getQuantity() < request.quantity()) {
                items.add(new ItemStockStatus(
                        request.productId(),
                        false,
                        request.quantity(),
                        product.getQuantity(),
                        "Insufficient stock for product: " + product.getName()));

                continue;
            }

            items.add(new ItemStockStatus(
                    request.productId(),
                    true,
                    request.quantity(),
                    product.getQuantity() - request.quantity(),
                    ""));
        }

        boolean allSuccessful = items.stream()
                .allMatch(ItemStockStatus::success);

        if (!allSuccessful) {
            return new StockUpdateResult(false, items);
        }

        for (StockRequest request : StockRequest) {

            Product product = productRepository
                    .findById(request.productId())
                    .orElseThrow();

            int newQuantity = product.getQuantity() - request.quantity();

            product.setQuantity(newQuantity);
            productRepository.save(product);
        }

        return new StockUpdateResult(true, items);
    }

    public void restockStock(List<StockRequest> stockRequests) {
        for (StockRequest request : stockRequests) {
            productRepository.findById(request.productId()).ifPresent(product -> {
                product.setQuantity(product.getQuantity() + request.quantity());
                productRepository.save(product);
            });
        }
    }

}
