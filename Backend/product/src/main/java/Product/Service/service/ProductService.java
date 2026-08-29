package Product.Service.service;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import Product.Service.dto.ProductRequest;
import Product.Service.dto.ProductResponse;
import Product.Service.dto.kafka.ProductCreated;
import Product.Service.dto.kafka.ProductDeleted;
import Product.Service.exception.ForbiddenException;
import Product.Service.exception.ProductNotFoundException;
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

    private final KafkaTemplate<String, Object> kafka;

    public ProductResponse getProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        return toResponse(product);
    }

    public List<ProductResponse> getAllProduct() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse createProduct(ProductRequest productRequest, String userId) {
        Product product = Product.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .quantity(productRequest.quantity())
                .userId(userId)
                .build();
        product = productRepository.save(product);
        ProductCreated event = new ProductCreated(product.getId(), userId);
        kafka.send("product.created", userId, event);
        System.out.println("====================================\nProduct Created Event Lunched");
        return toResponse(product);
    }

    public ProductResponse updateProduct(ProductRequest productRequest, String id, String userId) {
        Product product = ownedProductOrThrow(id, userId);

        product.setName(productRequest.name());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());
        product.setQuantity(productRequest.quantity());
        productRepository.save(product);
        System.out.println("====================================\nProduct Updated Event Lunched");
        System.out.println("====================================\n" + product.getImageUrls());
        return toResponse(product);
    }

    public void deleteProduct(String id, String userId) {
        ownedProductOrThrow(id, userId);
        ProductDeleted event = new ProductDeleted(id);
        kafka.send("product.deleted", id, event);
        productRepository.deleteById(id);
    }

    public void addImageUrl(String productId, List<String> imageUrls) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        // product.getImageUrls().add(imageUrl);
        product.setImageUrls(imageUrls);
        productRepository.save(product);
    }

    public void removeImageUrl(String productId, String url) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        List<String> urls = product.getImageUrls();
        urls.remove(url);
        System.out.println("====================================\nurls = " + urls);
        product.setImageUrls(urls);
        productRepository.save(product);
    }

    public List<ProductResponse> getMyProduct(String userId) {
        return productRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
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

}