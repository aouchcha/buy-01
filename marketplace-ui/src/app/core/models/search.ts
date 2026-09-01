import { Category } from './product';

export type SortBy = 'price_asc' | 'price_desc' | 'newest';

export const SORT_OPTIONS: { value: SortBy; label: string }[] = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
];

export interface SearchParams {
  keyword?: string;
  category?: Category;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: SortBy;
  page?: number;
  size?: number;
}

export interface ProductDocument {
  id: string;
  productName: string;
  description: string;
  category: string;
  sellerId: string;
  price: number;
  createdAt: string;
  imageUrls: string[];
  quantity: number;
}
