package Product.Service.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import Product.Service.dto.kafka.ProductImageDeletedEvent;
import Product.Service.dto.kafka.ProductImageUploadedEvent;
import Product.Service.service.ProductService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductMediaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductMediaEventConsumer.class);

    private final ProductService productService;

    @KafkaListener(topics = "product.upload.seccess", groupId = "product-service")
    public void onImageUploaded(ProductImageUploadedEvent event) {
        log.info("Image uploaded for product {}: {}", event.productId(), event.imageUrls());
        productService.addImageUrl(event.productId(), event.imageUrls());
    }

    @KafkaListener(topics = "product.media.deleted")
    public void onImageDeleted(ProductImageDeletedEvent event) {
        productService.removeImageUrl(event.productId(), event.url());
    }

    // @KafkaListener(topics = "media.product.failed", groupId = "product-service")
    // public void onImageFailed(ProductImageUploadedEvent event) {
    //     log.warn("Image upload failed for product {}: {}", event.productId(), event.imageUrls());
    // }
}
