package service.search.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOrder;
import service.search.model.ProductDocument;

@Service
public class SearchService {
    private final ElasticsearchOperations operations;

    public SearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    public List<ProductDocument> search(String keyword, String category, Double minPrice, Double maxPrice,
            String sortBy, int page, int size) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        
        //Search part
        if (keyword != null && !keyword.isBlank()) {
            boolQuery.must(m -> 
                m.multiMatch(mm -> 
                    mm.query(keyword).fields("productName^2", "description") // ^2 = name matches weigh more
                )
            );
        }

        //Filter part
        if (category != null) {
            boolQuery.filter(f ->
                f.term(t ->
                    t.field("category").value(category)
                )
            );
        }

        if (maxPrice != null || minPrice != null) {
            boolQuery.filter(f -> 
                f.range(r -> r.number(n -> {
                    n.field("price");
                    if (minPrice != null) n.gte(minPrice);
                    if (maxPrice != null) n.lte(maxPrice);
                    return n;
                }))
            );
        }

        Query query = Query.of(q -> q.bool(boolQuery.build()));

        Pageable pageable = PageRequest.of(page, size);
    
        NativeQueryBuilder nativeQueryBuilder = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable);

        if (sortBy != null) {
            // e.g. "price_asc", "price_desc", "newest"
            switch (sortBy) {
                case "price_asc" -> nativeQueryBuilder.withSort(s -> s.field(f -> f
                        .field("price").order(SortOrder.Asc)));
                case "price_desc" -> nativeQueryBuilder.withSort(s -> s.field(f -> f
                        .field("price").order(SortOrder.Desc)));
                case "newest" -> nativeQueryBuilder.withSort(s -> s.field(f -> f
                        .field("createdAt").order(SortOrder.Desc)));
                default -> nativeQueryBuilder.withSort(s -> s.field(f -> f
                        .field("price").order(SortOrder.Asc)));
            }
        }

        SearchHits<ProductDocument> hits = operations.search(nativeQueryBuilder.build(), ProductDocument.class);
        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        }
}
