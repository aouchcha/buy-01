package Product.Service.service;

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

        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId());
    }

    public List<ProductResponse> getAllProduct() {
        return productRepository.findAll().stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getQuantity(),
                        p.getUserId()))
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
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId());
    }

    public ProductResponse updateProduct(ProductRequest productRequest, String id, String userId) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        if (!product.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this product");
        }

        product.setName(productRequest.name());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());
        product.setQuantity(productRequest.quantity());
        productRepository.save(product);
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId());
    }

    public void deleteProduct(String id, String userId) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        if (!product.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this product");
        }

        productRepository.deleteById(id);
    }

    public List<ProductResponse> getMyProduct(String userId) {
        return productRepository.findByUserId(userId).stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getQuantity(),
                        p.getUserId()))
                .toList();
    }

}