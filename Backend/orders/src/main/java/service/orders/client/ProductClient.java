package service.orders.client;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import service.orders.dto.ProductResponse;
import service.orders.dto.StockUpdateResult;
import service.orders.exception.ProductNotFoundException;
import service.orders.dto.ProductRequest;
import java.util.List;

@Component
@AllArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    public ProductResponse getProduct(String productId) {
        try {
            return restTemplate.getForObject(
                    "http://product-service/api/product/{id}",
                    ProductResponse.class,
                    productId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException("Product not found: " + productId);
        }
    }

   public StockUpdateResult updateProductStock(List<ProductRequest> productRequests) {
    try {
        return restTemplate.patchForObject(
                "http://product-service/api/product/update-stock",
                productRequests,
                StockUpdateResult.class);
    } catch (HttpClientErrorException.BadRequest e) {
        // Deserialize the JSON body containing partial/full failure details
        StockUpdateResult result = e.getResponseBodyAs(StockUpdateResult.class);
        throw new PartialOutOfStockException("Some items in cart are out of stock", result);
    }
}

  
}
