
export interface ProductDto {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId: string;
  imageUrls: string[];
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
}