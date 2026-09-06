export type AnalyticsPeriod = 'today' | 'week' | 'month';

export const ANALYTICS_PERIOD_OPTIONS: { value: AnalyticsPeriod; label: string }[] = [
  { value: 'today', label: 'Today' },
  { value: 'week', label: 'This week' },
  { value: 'month', label: 'This month' },
];

export interface BestSellingProduct {
  productId: string;
  productName: string;
  totalUnitsSold: number | null;
}

export interface Analytics {
  bestSellingProducts: BestSellingProduct[];
  total: number;
}
