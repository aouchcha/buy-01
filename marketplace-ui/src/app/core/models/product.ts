
export interface ProductDto {
  id: number;
  name: string;
  description: string;
  price: number;
  quantity: number;
  imageUrl?: string;
  createdAt: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
}