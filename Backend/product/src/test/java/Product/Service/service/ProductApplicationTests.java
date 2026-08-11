package Product.Service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import Product.Service.dto.ProductRequest;
import Product.Service.dto.ProductResponse;
import Product.Service.dto.kafka.ProductCreated;
import Product.Service.dto.kafka.ProductDeleted;
import Product.Service.exception.ProductNotFoundException;
import Product.Service.model.Product;
import Product.Service.repository.ProductRepository;
import Product.Service.exception.ForbiddenException;
import java.util.List;

// @SpringBootTest
@ExtendWith(MockitoExtension.class)
class ProductApplicationTests {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private KafkaTemplate<String, Object> kafka;

	@InjectMocks
	private ProductService productService;

	private Product product;
	private ProductRequest request;

	@BeforeEach
	void setUp() {

		product = Product.builder()
				.id("1")
				.name("dajaja")
				.description("dajaja bldya dyl bayd")
				.price(30)
				.quantity(500)
				.userId("user1")
				.build();

		request = new ProductRequest(
				"flos",
				"Smart flos",
				800.0,
				3);
	}

	@Test
	void shouldReturnProduct() {

		when(productRepository.findById("1"))
				.thenReturn(Optional.of(product));

		ProductResponse response = productService.getProduct("1");

		assertNotNull(response);
		assertEquals("1", response.id());
		assertEquals("dajaja", response.name());

		verify(productRepository).findById("1");
	}

	@Test
	void shouldThrowWhenProductNotFound() {

		when(productRepository.findById("2"))
				.thenReturn(Optional.empty());

		assertThrows(
				ProductNotFoundException.class,
				() -> productService.getProduct("2"));
	}

	@Test
	void shouldReturnAllProducts() {

		when(productRepository.findAll())
				.thenReturn(List.of(product));

		List<ProductResponse> result = productService.getAllProduct();

		assertEquals(1, result.size());
		assertEquals("dajaja", result.get(0).name());

		verify(productRepository).findAll();
	}

	// create Product
	@Test
	void shouldCreateProduct() {

		when(productRepository.save(any(Product.class)))
				.thenAnswer(invocation -> {

					Product p = invocation.getArgument(0);
					p.setId("10");

					return p;
				});

		ProductResponse response = productService.createProduct(request, "user1-77");

		assertEquals("10", response.id());
		assertEquals("flos", response.name());

		verify(productRepository).save(any(Product.class));

		verify(kafka).send(
				eq("product.created"),
				eq("user1-77"),
				any(ProductCreated.class));
	}

	// update product
	@Test
	void shouldUpdateProduct() {

		when(productRepository.findByIdAndUserId("1", "user1-77"))
				.thenReturn(Optional.of(product));

		ProductResponse response = productService.updateProduct(request, "1", "user1-77");

		assertEquals("flos", response.name());
		assertEquals(800, response.price());

		verify(productRepository).save(product);
	}

	@Test
	void shouldThrowForbiddenWhenUserIsNotOwner() {

		when(productRepository.findByIdAndUserId("1", "user2"))
				.thenReturn(Optional.empty());

		when(productRepository.existsById("1"))
				.thenReturn(true);

		assertThrows(
				ForbiddenException.class,
				() -> productService.updateProduct(request, "1", "user2"));
	}

	@Test
	void shouldThrowProductNotFoundWhenUpdating() {

		when(productRepository.findByIdAndUserId("1", "user1-77"))
				.thenReturn(Optional.empty());

		when(productRepository.existsById("1"))
				.thenReturn(false);

		assertThrows(
				ProductNotFoundException.class,
				() -> productService.updateProduct(request, "1", "user1-77"));
	}

	// delet product
	@Test
	void shouldDeleteProduct() {

		when(productRepository.findByIdAndUserId("1", "user1-77"))
				.thenReturn(Optional.of(product));

		productService.deleteProduct("1", "user1-77");

		verify(kafka).send(
				eq("product.deleted"),
				eq("1"),
				any(ProductDeleted.class));

		verify(productRepository).deleteById("1");
	}

	// Add Image Urls

	@Test
	void shouldAddImageUrls() {

		when(productRepository.findById("1"))
				.thenReturn(Optional.of(product));

		List<String> images = List.of("https://a.jpg", "https://b.jpg");

		productService.addImageUrl("1", images);

		assertEquals(images, product.getImageUrls());

		verify(productRepository).save(product);
	}

	@Test
	void shouldThrowWhenAddingImageToUnknownProduct() {

		when(productRepository.findById("1"))
				.thenReturn(Optional.empty());

		assertThrows(
				ProductNotFoundException.class,
				() -> productService.addImageUrl("1", List.of()));
	}




    // remove ImageUrl

    @Test
    void shouldRemoveImageUrl() {

        product.setImageUrls(
                new java.util.ArrayList<>(
                        List.of("https://a.jpg", "https://b.jpg")
                )
        );

        when(productRepository.findById("1"))
                .thenReturn(Optional.of(product));

        productService.removeImageUrl("1", "https://a.jpg");

        assertFalse(product.getImageUrls().contains("https://a.jpg"));
        assertEquals(1, product.getImageUrls().size());

        verify(productRepository).save(product);
    }

    // get My Product

    @Test
    void shouldReturnUserProducts() {

        when(productRepository.findByUserId("user1-77"))
                .thenReturn(List.of(product));

        List<ProductResponse> result =
                productService.getMyProduct("user1-77");

        assertEquals(1, result.size());

        verify(productRepository).findByUserId("user1-77");
    }

    // delete product edge cases

    @Test
    void shouldThrowForbiddenWhenDeletingOtherUserProduct() {

        when(productRepository.findByIdAndUserId("1", "user2"))
                .thenReturn(Optional.empty());

        when(productRepository.existsById("1"))
                .thenReturn(true);

        assertThrows(
                ForbiddenException.class,
                () -> productService.deleteProduct("1", "user2"));
    }

    @Test
    void shouldThrowProductNotFoundWhenDeletingNonExistentProduct() {

        when(productRepository.findByIdAndUserId("999", "user1-77"))
                .thenReturn(Optional.empty());

        when(productRepository.existsById("999"))
                .thenReturn(false);

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct("999", "user1-77"));
    }

    // removeImageUrl edge case

    @Test
    void shouldThrowWhenRemovingImageFromUnknownProduct() {

        when(productRepository.findById("999"))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.removeImageUrl("999", "https://a.jpg"));
    }
}
