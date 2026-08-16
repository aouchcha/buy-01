export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
}

export enum PaymentMethod {
  CASH_ON_DELIVERY = 'CASH_ON_DELIVERY',
}

export interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string | null;
  price: number;
  quantity: number;
  totalPrice: number;
}

export interface Order {
  id: string;
  userId: string;
  status: OrderStatus;
  totalAmount: number;
  fullName: string;
  address: string;
  city: string;
  phoneNumber: string;
  createdAt: number;
  cartItems: OrderItem[];
  paymentMethod: PaymentMethod;
}

export interface CreateOrderItemRequest {
  productId: string;
  productName: string;
  price: number;
  quantity: number;
}

export interface CreateOrderRequest {
  fullName: string;
  phoneNumber: string;
  city: string;
  address: string;
  items: CreateOrderItemRequest[];
}
