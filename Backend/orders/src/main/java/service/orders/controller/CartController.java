package service.orders.controller;

import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import service.orders.models.Cart;
import service.orders.service.CartService;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import service.orders.dto.CartItemsRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;


@RestController
@AllArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;


    @GetMapping
    public ResponseEntity<Cart> getCartItems(@RequestHeader("X-User-Id") String userId) {
        Cart cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItemToCart(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody CartItemsRequest cartItem) {
        System.out.println("=================\nReceived request to add item to cart: " + cartItem);
        cartService.addItemToCart(userId, cartItem);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/items")
    public ResponseEntity<Cart> updateCartItemQuantity(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody CartItemsRequest cartItem) {
        Cart cart = cartService.updateCartItemQuantity(userId, cartItem);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItemFromCart(@RequestHeader("X-User-Id") String userId, @PathVariable String productId) {
        Cart cart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}