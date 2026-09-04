package service.search.handler;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import service.search.model.ProductDocument;
import service.search.service.SearchService;

@RestController
@RequiredArgsConstructor
public class searchController {
    private final SearchService searchService;

    @GetMapping("/api/search/products")
    public List<ProductDocument> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return searchService.search(keyword, category, minPrice, maxPrice, sortBy, page, size);
    }
}
