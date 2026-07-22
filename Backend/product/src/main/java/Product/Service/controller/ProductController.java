package Product.Service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Product.Service.dto.ProductRequest;
import Product.Service.dto.ProductResponse;
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
        ProductResponse productResponse = productService.getProduct(id);
        return ResponseEntity.ok(productResponse);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProduct() {
        List<ProductResponse> productResponse = productService.getAllProduct();
        return ResponseEntity.ok(productResponse);
    }

    @PostMapping
    public ResponseEntity<String> createProduct(){
        return  ResponseEntity.ok("hello =>");
    }
    // public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest,
    //         @AuthenticationPrincipal Jwt jwt) {
    //     String userId = jwt.getSubject();
    //     ProductResponse productResponse = productService.createProduct(productRequest, userId);
    //     return ResponseEntity.ok(productResponse);
    // }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@Valid @RequestBody ProductRequest productRequest,
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        ProductResponse productResponse = productService.updateProduct(productRequest, id, userId);
        return ResponseEntity.ok(productResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        productService.deleteProduct(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/myProducts")
    public ResponseEntity<List<ProductResponse>> getMyProduct(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<ProductResponse> productResponse = productService.getMyProduct(userId);
        return ResponseEntity.ok(productResponse);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("product-service is running");
    }

}