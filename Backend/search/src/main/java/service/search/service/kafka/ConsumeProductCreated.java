package service.search.service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import service.search.dto.kafka.ProductCreatedToES;
import service.search.dto.kafka.ProductDeletion;
import service.search.model.ProductDocument;
// import service.search.dto.kafka.ProductCearted;
import service.search.repository.SearchRepository;

@Service
@RequiredArgsConstructor
public class ConsumeProductCreated {
    private final SearchRepository searchRepository;

    @KafkaListener(topics = "product.created.ES", groupId = "es-service")
    public void onProductCreation(ProductCreatedToES event) {
        final ProductDocument productDocument = MapperToProductDocument(event);
        searchRepository.save(productDocument);
    }

    @KafkaListener(topics = "product.deleted.ES", groupId = "es-service")
    public void onProductDeletion(ProductDeletion event) {
        searchRepository.deleteById(event.productId());
    }

    private ProductDocument MapperToProductDocument(ProductCreatedToES productCreatedToES) {
        ProductDocument productDocument = ProductDocument.builder().
            id(productCreatedToES.productId())
            .productName(productCreatedToES.name())
            .description(productCreatedToES.description())
            .category(productCreatedToES.category())
            .sellerId(productCreatedToES.userId())
            .price(productCreatedToES.price())
            .createdAt(productCreatedToES.createdAt())
            .imageUrls(productCreatedToES.imageUrls())
            .quantity(productCreatedToES.quantity())
            .build();
            return productDocument;
    }
}
