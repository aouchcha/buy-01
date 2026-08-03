// package Product.Service.controller;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.doNothing;
// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// import java.util.List;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.context.annotation.Import;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;

// import com.fasterxml.jackson.databind.ObjectMapper;

// import Product.Service.config.HeaderAuthFilter;
// import Product.Service.config.SecurityConfig;
// import Product.Service.dto.ProductRequest;
// import Product.Service.dto.ProductResponse;
// import Product.Service.service.ProductService;

// @WebMvcTest(ProductController.class)
// @Import({ SecurityConfig.class, HeaderAuthFilter.class })
// class ProductControllerWebMvcTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @MockBean
//     private ProductService productService;

//     @Autowired
//     private ObjectMapper objectMapper;

//     private ProductResponse productResponse;
//     private ProductRequest validRequest;

//     @BeforeEach
//     void setUp() {
//         productResponse = new ProductResponse(
//                 "1", "Laptop", "A powerful laptop for developers", 1200.0, 5, "user1", List.of());
//         validRequest = new ProductRequest(
//                 "Laptop", "A powerful laptop for developers", 1200.0, 5);
//     }

//     // ── GET endpoints : publics, pas besoin d'auth ───────────────────────────

//     @Test
//     void shouldAllowGetProduct_withoutAuth() throws Exception {
//         when(productService.getProduct("1")).thenReturn(productResponse);

//         mockMvc.perform(get("/api/product/1"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.id").value("1"));
//     }

//     @Test
//     void shouldAllowGetAllProducts_withoutAuth() throws Exception {
//         when(productService.getAllProduct()).thenReturn(List.of(productResponse));

//         mockMvc.perform(get("/api/product"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.length()").value(1));
//     }

//     @Test
//     void shouldAllowHealth_withoutAuth() throws Exception {
//         mockMvc.perform(get("/api/product/health"))
//                 .andExpect(status().isOk());
//     }

//     // ── POST : requiert ROLE_SELLER ──────────────────────────────────────────

//     @Test
//     void shouldReturn401_whenCreatingProduct_withoutUserId() throws Exception {
//         // Pas de X-User-Id → pas d'authentification → 401
//         mockMvc.perform(post("/api/product")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(validRequest)))
//                 .andExpect(status().isUnauthorized());
//     }

//     @Test
//     void shouldReturn403_whenCreatingProduct_withoutSellerRole() throws Exception {
//         // X-User-Id présent mais pas de rôle SELLER → 403
//         mockMvc.perform(post("/api/product")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(validRequest))
//                         .header("X-User-Id", "user1"))
//                 .andExpect(status().isForbidden());
//     }

//     @Test
//     void shouldReturn403_whenCreatingProduct_withBuyerRole() throws Exception {
//         mockMvc.perform(post("/api/product")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(validRequest))
//                         .header("X-User-Id", "user1")
//                         .header("X-User-Role", "BUYER"))
//                 .andExpect(status().isForbidden());
//     }

//     @Test
//     void shouldCreateProduct_whenRoleIsSeller() throws Exception {
//         when(productService.createProduct(any(ProductRequest.class), eq("user1")))
//                 .thenReturn(productResponse);

//         mockMvc.perform(post("/api/product")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(validRequest))
//                         .header("X-User-Id", "user1")
//                         .header("X-User-Role", "SELLER"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.id").value("1"));
//     }

//     // ── PUT : requiert ROLE_SELLER ───────────────────────────────────────────

//     @Test
//     void shouldReturn401_whenUpdatingProduct_withoutUserId() throws Exception {
//         mockMvc.perform(put("/api/product/1")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(validRequest)))
//                 .andExpect(status().isUnauthorized());
//     }

//     @Test
//     void shouldReturn403_whenUpdatingProduct_withoutSellerRole() throws Exception {
//         mockMvc.perform(put("/api/product/1")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(validRequest))
//                         .header("X-User-Id", "user1"))
//                 .andExpect(status().isForbidden());
//     }

//     @Test
//     void shouldUpdateProduct_whenRoleIsSeller() throws Exception {
//         when(productService.updateProduct(any(ProductRequest.class), eq("1"), eq("user1")))
//                 .thenReturn(productResponse);

//         mockMvc.perform(put("/api/product/1")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(validRequest))
//                         .header("X-User-Id", "user1")
//                         .header("X-User-Role", "SELLER"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.name").value("Laptop"));
//     }

//     // ── DELETE : requiert ROLE_SELLER ────────────────────────────────────────

//     @Test
//     void shouldReturn401_whenDeletingProduct_withoutUserId() throws Exception {
//         mockMvc.perform(delete("/api/product/1"))
//                 .andExpect(status().isUnauthorized());
//     }

//     @Test
//     void shouldReturn403_whenDeletingProduct_withoutSellerRole() throws Exception {
//         mockMvc.perform(delete("/api/product/1")
//                         .header("X-User-Id", "user1"))
//                 .andExpect(status().isForbidden());
//     }

//     @Test
//     void shouldDeleteProduct_whenRoleIsSeller() throws Exception {
//         doNothing().when(productService).deleteProduct("1", "user1");

//         mockMvc.perform(delete("/api/product/1")
//                         .header("X-User-Id", "user1")
//                         .header("X-User-Role", "SELLER"))
//                 .andExpect(status().isNoContent());
//     }
// }
