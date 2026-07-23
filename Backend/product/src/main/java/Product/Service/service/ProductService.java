package Product.Service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import Product.Service.dto.ProductRequest;
import Product.Service.dto.ProductResponse;
import Product.Service.exception.ForbiddenException;
import Product.Service.exception.ProductNotFoundException;
import Product.Service.model.Product;
import Product.Service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final String PRODUCT_NOT_FOUND = "Product not found";

    private final ProductRepository productRepository;

    public ProductResponse getProduct(String id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

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
        productRepository.save(product);
        return toResponse(product);
    }

    public ProductResponse updateProduct(ProductRequest productRequest, String id, String userId) {
        Product product = ownedProductOrThrow(id, userId);

        product.setName(productRequest.name());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());
        product.setQuantity(productRequest.quantity());
        productRepository.save(product);
        return toResponse(product);
    }

    public void deleteProduct(String id, String userId) {
        ownedProductOrThrow(id, userId);
        productRepository.deleteById(id);
    }

    public void addImageUrl(String productId, String imageUrl) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        product.getImageUrls().add(imageUrl);
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
                product.getQuantity(), product.getUserId(), product.getImageUrls());
    }

}