package service.orders.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import service.orders.client.ProductClient;
import service.orders.models.Cart;
import service.orders.repository.CartRepository;
import java.util.ArrayList;
import java.util.Optional;
import service.orders.models.CartItems;
import service.orders.dto.CartItemsRequest;
import service.orders.dto.ProductResponse;
import service.orders.exception.InsufficientStockException;
import service.orders.exception.CartItemNotFoundException;

@Service
@AllArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public Cart getCart(String userId) {

        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);
        Cart cart = optionalCart.orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setCartItems(new ArrayList<>());
            return cartRepository.save(newCart);
        });

        for (CartItems item : cart.getCartItems()) {
            try {
                ProductResponse product = productClient.getProduct(item.getProductId());
                item.setOutOfStock(product.quantity() < item.getQuantity());
                System.out.println("========================product.quantity() < item.getQuantity()===========================");
                System.out.println(product.quantity() < item.getQuantity());

            } catch (Exception e) {
                item.setOutOfStock(true); 
            }
        }

        return cartRepository.save(cart);
    }

    public void addItemToCart(String userId, CartItemsRequest cartItemRequest) {
        ProductResponse product = productClient.getProduct(cartItemRequest.productId());

        if (product.quantity() < cartItemRequest.quantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.name());
        }

        Cart cart = getCart(userId);
        Optional<CartItems> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(product.id()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItems cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + cartItemRequest.quantity();
            if (product.quantity() < newQuantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.name());
            }
            cartItem.setQuantity(newQuantity);
            cartItem.setTotalPrice(product.price() * newQuantity);
        } else {
            CartItems cartItem = CartItems.builder()
                    .sellerId(product.userId())
                    .productId(product.id())
                    .productName(product.name())
                    .price(product.price())
                    .quantity(cartItemRequest.quantity())
                    .totalPrice(product.price() * cartItemRequest.quantity())
                    .build();
            cart.getCartItems().add(cartItem);
        }
        cartRepository.save(cart);
    }

    public Cart updateCartItemQuantity(String userId, CartItemsRequest cartItemRequest) {
        ProductResponse product = productClient.getProduct(cartItemRequest.productId());

        if (product.quantity() < cartItemRequest.quantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.name());
        }

        Cart cart = getCart(userId);
        Optional<CartItems> optionalCartItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(cartItemRequest.productId()))
                .findFirst();

        CartItems cartItem = optionalCartItem.orElseThrow(
                () -> new CartItemNotFoundException("Product not found in the cart: " + product.name()));
        cartItem.setQuantity(cartItemRequest.quantity());
        cartItem.setTotalPrice(product.price() * cartItemRequest.quantity());
        return cartRepository.save(cart);
    }

    public Cart removeItemFromCart(String userId, String productId) {
        Cart cart = getCart(userId);
        cart.getCartItems().removeIf(item -> item.getProductId().equals(productId));
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        Cart cart = getCart(userId);
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }
}
