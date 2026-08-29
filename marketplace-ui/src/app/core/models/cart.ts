export interface CartItem {
  id: string;
  sellerId: string | null;
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  totalPrice: number;
  OutOfStock?: boolean;
}

export interface Cart {
  id: string;
  userId: string;
  cartItems: CartItem[];
}


