export interface OrderItem {
  productId: string;
  quantity: number;
  unitPrice: number;
}

export interface OrderRequest {
  customerId: string;
  items: OrderItem[];
}

export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'PAYMENT_FAILED';

export interface OrderResponse {
  id: string;
  customerId: string;
  status: OrderStatus;
  totalAmount: number;
  currency: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  sku: string;
  isActive: boolean;
}

export interface Stock {
  productId: string;
  availableQty: number;
  reservedQty: number;
  totalQty: number;
  reorderLevel: number;
  isLowStock: boolean;
  lastUpdated: string;
}

export interface Transaction {
  id: string;
  orderId: string;
  amount: number;
  currency: string;
  gatewayRef: string;
  status: string;
  createdAt: string;
}

export interface Shipment {
  id: string;
  orderId: string;
  trackingNumber: string;
  carrier: string;
  status: string;
  estimatedDelivery: string;
  actualDeliveryDate: string | null;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface AuthState {
  token: string | null;
  userId: string | null;
  role: string | null;
  isAuthenticated: boolean;
}

export interface OrderMetric {
  metricHour: string;
  totalOrders: number;
  confirmedOrders: number;
  shippedOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  totalRevenue: number;
  avgOrderValue: number;
}

export interface RevenueRecord {
  revenueDate: string;
  totalRevenue: number;
  totalOrders: number;
  paymentSuccessCount: number;
  paymentFailureCount: number;
}

export interface FailureSummary {
  totalFailures: number;
  paymentFailures: number;
  inventoryFailures: number;
  events: unknown[];
}

export interface ConsumerLag {
  consumerGroup: string;
  totalLag: number;
  metrics: Record<string, { topic: string; partition: number; lag: number }>;
}
