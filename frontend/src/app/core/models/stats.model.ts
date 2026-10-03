export interface DashboardStats {
  totalRevenue: number;
  totalOrders: number;
  totalProducts: number;
  totalCustomers: number;
  lowStockCount: number;
  pendingOrdersCount: number;
  productsByCategory: { [category: string]: number };
  monthlySales: { [month: string]: number };
}
