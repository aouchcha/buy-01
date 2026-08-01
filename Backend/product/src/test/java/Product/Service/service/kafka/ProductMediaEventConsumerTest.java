package Product.Service.service.kafka;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Product.Service.dto.kafka.ProductImageDeletedEvent;
import Product.Service.dto.kafka.ProductImageUploadedEvent;
import Product.Service.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductMediaEventConsumerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductMediaEventConsumer consumer;

    @Test
    void shouldCallAddImageUrl_whenImageUploaded() {
        List<String> urls = List.of("https://cdn.example.com/img1.jpg", "https://cdn.example.com/img2.jpg");
        ProductImageUploadedEvent event = new ProductImageUploadedEvent("user1", "product1", urls);

        consumer.onImageUploaded(event);

        verify(productService).addImageUrl("product1", urls);
    }

    @Test
    void shouldCallRemoveImageUrl_whenImageDeleted() {
        ProductImageDeletedEvent event = new ProductImageDeletedEvent("product1", "user1", "https://cdn.example.com/img1.jpg");

        consumer.onImageDeleted(event);

        verify(productService).removeImageUrl("product1", "https://cdn.example.com/img1.jpg");
    }
}
