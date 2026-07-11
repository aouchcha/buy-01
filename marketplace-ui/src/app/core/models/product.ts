
export type ProductCategory = 'chicken' | 'pigeon' | 'ostrich' | 'exotic';

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  image?: string;
  category: ProductCategory;
}