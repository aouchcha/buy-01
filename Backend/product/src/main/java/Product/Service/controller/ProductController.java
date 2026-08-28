package Product.Service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

import Product.Service.dto.ProductRequest;
import Product.Service.dto.ProductResponse;
import Product.Service.dto.OrderRequest;
import Product.Service.service.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/product")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProduct() {
        System.out.println("============================================================================\n===============++=================++++==============++=========================\n=============================\n");
        return ResponseEntity.ok(productService.getAllProduct());
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest productRequest,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(productService.createProduct(productRequest, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Valid @RequestBody ProductRequest productRequest,
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(productService.updateProduct(productRequest, id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        productService.deleteProduct(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/myProducts")
    public ResponseEntity<List<ProductResponse>> getMyProduct(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(productService.getMyProduct(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("product-service is running");
    }

    @PatchMapping("/update-stock")
    public ResponseEntity<Void> updateStock(@RequestBody List<OrderRequest> productRequests) {
        productService.updateStock(productRequests);
        return ResponseEntity.noContent().build();
    }
}
