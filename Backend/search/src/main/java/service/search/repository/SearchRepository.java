package service.search.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import service.search.model.ProductDocument;

@Repository
public interface SearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    // used by the Kafka consumer to upsert/delete individual documents
    // (save() and deleteById() come from the base interface — nothing to add here)
}
