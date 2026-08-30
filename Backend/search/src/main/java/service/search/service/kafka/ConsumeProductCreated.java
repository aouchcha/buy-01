package service.search.service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import service.search.dto.kafka.ProductCreated;
import service.search.dto.kafka.ProductDeletion;
import service.search.model.ProductDocument;
// import service.search.dto.kafka.ProductCearted;
import service.search.repository.SearchRepository;

@Service
@RequiredArgsConstructor
public class ConsumeProductCreated {
    private final SearchRepository searchRepository;

    @KafkaListener(topics = "product.created.ES", groupId = "es-service")
    public void onProductCreation(ProductCreated event) {
        final ProductDocument productDocument = MapperToProductDocument(event);
        searchRepository.save(productDocument);
    }

    @KafkaListener(topics = "product.deleted.ES", groupId = "es-service")
    public void onProductDeletion(ProductDeletion event) {
        searchRepository.deleteById(event.productId());
    }

    private ProductDocument MapperToProductDocument(ProductCreated productCreated) {
        ProductDocument productDocument = ProductDocument.builder().
            id(productCreated.productId())
            .productName(productCreated.name())
            .description(productCreated.description())
            .category(productCreated.category())
            .sellerId(productCreated.userId())
            .price(productCreated.price())
            .createdAt(productCreated.createdAt())
            .imageUrls(productCreated.imageUrls())
            .quantity(productCreated.quantity())
            .build();
            return productDocument;
    }
}
