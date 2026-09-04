
export enum Category {
  LIVE_POULTRY = 'LIVE_POULTRY',
  CHICKS = 'CHICKS',
  EXOTIC_BIRDS = 'EXOTIC_BIRDS',
  CONSUMPTION_EGGS = 'CONSUMPTION_EGGS',
  HATCHING_EGGS = 'HATCHING_EGGS',
  SPECIALTY_EGGS = 'SPECIALTY_EGGS',
  FEED_AND_SUPPLIES = 'FEED_AND_SUPPLIES',
}

export const CATEGORY_LABELS: Record<Category, string> = {
  [Category.LIVE_POULTRY]: 'Live Poultry',
  [Category.CHICKS]: 'Chicks & Ducklings',
  [Category.EXOTIC_BIRDS]: 'Exotic & Ornamental Birds',
  [Category.CONSUMPTION_EGGS]: 'Fresh Eggs',
  [Category.HATCHING_EGGS]: 'Hatching Eggs',
  [Category.SPECIALTY_EGGS]: 'Specialty Eggs',
  [Category.FEED_AND_SUPPLIES]: 'Feed & Equipment',
};

export const CATEGORY_OPTIONS: { value: Category; label: string }[] =
  Object.values(Category).map((value) => ({ value, label: CATEGORY_LABELS[value] }));

export interface ProductDto {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId: string;
  category: Category;
  imageUrls: string[];
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  category: Category;
}
