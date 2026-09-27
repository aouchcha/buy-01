package service.orders.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import service.orders.dto.ProductResponse;
import service.orders.dto.StockUpdateResult;
import service.orders.dto.stockRequests;
import service.orders.exception.ProductNotFoundException;
import java.util.List;
import service.orders.exception.PartialOutOfStockException;

@Component
@AllArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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

    public StockUpdateResult updateProductStock(List<stockRequests> productRequests) {
        try {
            return restTemplate.patchForObject(
                    "http://product-service/api/product/update-stock",
                    productRequests,
                    StockUpdateResult.class);
        } catch (HttpClientErrorException.BadRequest e) {
            // Deserialize the JSON body containing partial/full failure details
            StockUpdateResult result;
            try {
                result = objectMapper.readValue(e.getResponseBodyAsString(), StockUpdateResult.class);
            } catch (JsonProcessingException parseException) {
                throw new PartialOutOfStockException("Some items in cart are out of stock", null);
            }
            throw new PartialOutOfStockException("Some items in cart are out of stock", result);
        }
    }

    public void restockProductStock(List<stockRequests> productRequests) {
        restTemplate.patchForObject(
                "http://product-service/api/product/restock-stock",
                productRequests,
                Void.class);
    }

}
