import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '../store';
import type {
  OrderRequest,
  OrderResponse,
  Stock,
  Transaction,
  Shipment,
  OrderMetric,
  RevenueRecord,
  FailureSummary,
  ConsumerLag,
} from '../types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const api = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: API_URL,
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
  tagTypes: ['Order', 'Stock', 'Payment', 'Shipment', 'Analytics'],
  endpoints: (builder) => ({
    // ---- Orders ----
    createOrder: builder.mutation<OrderResponse, OrderRequest>({
      query: (body) => ({ url: '/api/orders', method: 'POST', body }),
      invalidatesTags: ['Order'],
    }),
    getOrder: builder.query<OrderResponse, string>({
      query: (id) => `/api/orders/${id}`,
      providesTags: ['Order'],
    }),
    getOrdersByCustomer: builder.query<OrderResponse[], string>({
      query: (customerId) => `/api/orders/customer/${customerId}`,
      providesTags: ['Order'],
    }),
    cancelOrder: builder.mutation<OrderResponse, string>({
      query: (id) => ({ url: `/api/orders/${id}/cancel`, method: 'PATCH' }),
      invalidatesTags: ['Order'],
    }),

    // ---- Inventory ----
    getStock: builder.query<Stock, string>({
      query: (productId) => `/api/inventory/${productId}`,
      providesTags: ['Stock'],
    }),
    updateStock: builder.mutation<Stock, { productId: string; availableQty: number; reorderLevel: number }>({
      query: ({ productId, availableQty, reorderLevel }) => ({
        url: `/api/inventory/${productId}?availableQty=${availableQty}&reorderLevel=${reorderLevel}`,
        method: 'PUT',
      }),
      invalidatesTags: ['Stock'],
    }),
    getLowStock: builder.query<Stock[], void>({
      query: () => '/api/inventory/low-stock',
      providesTags: ['Stock'],
    }),

    // ---- Payments ----
    getPaymentByOrder: builder.query<Transaction, string>({
      query: (orderId) => `/api/payments/order/${orderId}`,
      providesTags: ['Payment'],
    }),

    // ---- Shipping ----
    getShipmentByOrder: builder.query<Shipment, string>({
      query: (orderId) => `/api/shipping/order/${orderId}`,
      providesTags: ['Shipment'],
    }),

    // ---- Analytics ----
    getOrderSummary: builder.query<{ metrics: OrderMetric[]; count: number }, number>({
      query: (hoursBack) => `/api/analytics/orders/summary?hoursBack=${hoursBack}`,
      providesTags: ['Analytics'],
    }),
    getRevenueDaily: builder.query<{ records: RevenueRecord[]; count: number }, number>({
      query: (days) => `/api/analytics/revenue/daily?days=${days}`,
      providesTags: ['Analytics'],
    }),
    getFailures: builder.query<FailureSummary, void>({
      query: () => '/api/analytics/failures',
      providesTags: ['Analytics'],
    }),
    getConsumerLag: builder.query<ConsumerLag, void>({
      query: () => '/api/analytics/kafka/consumer-lag',
      providesTags: ['Analytics'],
    }),
  }),
});

export const {
  useCreateOrderMutation,
  useGetOrderQuery,
  useGetOrdersByCustomerQuery,
  useCancelOrderMutation,
  useGetStockQuery,
  useUpdateStockMutation,
  useGetLowStockQuery,
  useGetPaymentByOrderQuery,
  useGetShipmentByOrderQuery,
  useGetOrderSummaryQuery,
  useGetRevenueDailyQuery,
  useGetFailuresQuery,
  useGetConsumerLagQuery,
} = api;
