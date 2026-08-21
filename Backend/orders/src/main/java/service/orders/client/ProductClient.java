package service.orders.client;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import service.orders.dto.ProductResponse;
import service.orders.exception.ProductNotFoundException;

@Component
@AllArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    public ProductResponse getProduct(String productId) {
        try {
            return restTemplate.getForObject(
                    "http://product-service/api/product/{id}",
                    ProductResponse.class,
                    productId
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException("Product not found: " + productId);
        }
    }
}
