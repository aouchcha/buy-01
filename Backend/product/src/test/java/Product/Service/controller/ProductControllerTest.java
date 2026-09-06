package Product.Service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import Product.Service.dto.ProductRequest;
import Product.Service.dto.ProductResponse;
import Product.Service.exception.ForbiddenException;
import Product.Service.exception.GlobalExceptionHandler;
import Product.Service.exception.ProductNotFoundException;
import Product.Service.model.Category;
import Product.Service.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ProductResponse productResponse;
    private ProductRequest validRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        productResponse = new ProductResponse("1", "Laptop", "A powerful laptop for developers", 1200.0, 5, "user1", Category.FEED_AND_SUPPLIES, List.of());
        validRequest = new ProductRequest("Laptop", "A powerful laptop for developers", 1200.0, 5, Category.FEED_AND_SUPPLIES);
    }

    // GET /api/product/{id}

    @Test
    void shouldReturnProduct_whenFound() throws Exception {
        when(productService.getProduct("1")).thenReturn(productResponse);

        mockMvc.perform(get("/api/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void shouldReturn404_whenProductNotFound() throws Exception {
        when(productService.getProduct("999"))
                .thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(get("/api/product/999"))
                .andExpect(status().isNotFound());
    }

    // GET /api/product

    @Test
    void shouldReturnAllProducts() throws Exception {
        when(productService.getAllProduct()).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void shouldReturnEmptyList_whenNoProducts() throws Exception {
        when(productService.getAllProduct()).thenReturn(List.of());

        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // POST /api/product

    @Test
    void shouldCreateProduct() throws Exception {
        when(productService.createProduct(any(ProductRequest.class), eq("user1")))
                .thenReturn(productResponse);

        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void shouldReturn400_whenCreateRequestBodyIsInvalid() throws Exception {
        String invalidJson = """
                {"name": "", "description": "short", "price": -1.0, "quantity": 0}
                """;

        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .header("X-User-Id", "user1"))
                .andExpect(status().isBadRequest());
    }

    // PUT /api/product/{id}

    @Test
    void shouldUpdateProduct() throws Exception {
        when(productService.updateProduct(any(ProductRequest.class), eq("1"), eq("user1")))
                .thenReturn(productResponse);

        mockMvc.perform(put("/api/product/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void shouldReturn404_whenUpdatingNonExistentProduct() throws Exception {
        when(productService.updateProduct(any(ProductRequest.class), eq("999"), eq("user1")))
                .thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(put("/api/product/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("X-User-Id", "user1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403_whenUpdatingProductNotOwned() throws Exception {
        when(productService.updateProduct(any(ProductRequest.class), eq("1"), eq("other-user")))
                .thenThrow(new ForbiddenException("You do not own this product"));

        mockMvc.perform(put("/api/product/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("X-User-Id", "other-user"))
                .andExpect(status().isForbidden());
    }

    // DELETE /api/product/{id}

    @Test
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct("1", "user1");

        mockMvc.perform(delete("/api/product/1")
                        .header("X-User-Id", "user1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenDeletingNonExistentProduct() throws Exception {
        doThrow(new ProductNotFoundException("Product not found"))
                .when(productService).deleteProduct("999", "user1");

        mockMvc.perform(delete("/api/product/999")
                        .header("X-User-Id", "user1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403_whenDeletingProductNotOwned() throws Exception {
        doThrow(new ForbiddenException("You do not own this product"))
                .when(productService).deleteProduct("1", "other-user");

        mockMvc.perform(delete("/api/product/1")
                        .header("X-User-Id", "other-user"))
                .andExpect(status().isForbidden());
    }

    // GET /api/product/myProducts

    @Test
    void shouldReturnMyProducts() throws Exception {
        when(productService.getMyProduct("user1")).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/product/myProducts")
                        .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    // GET /api/product/health

    @Test
    void shouldReturnHealthOk() throws Exception {
        mockMvc.perform(get("/api/product/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("product-service is running"));
    }
}
