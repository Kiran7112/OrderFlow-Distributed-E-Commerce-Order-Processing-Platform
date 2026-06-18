import type { Product } from '../types';

// Sample products matching the seeded inventory_db (02_inventory_db.sql).
// In production these would be fetched from a product catalog endpoint.
export const MOCK_PRODUCTS: Product[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    name: 'Laptop Pro',
    description: 'High-performance laptop with 16GB RAM',
    price: 1299.99,
    category: 'Electronics',
    sku: 'LAPTOP-001',
    isActive: true,
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'Wireless Mouse',
    description: 'Ergonomic wireless mouse with USB receiver',
    price: 29.99,
    category: 'Accessories',
    sku: 'MOUSE-001',
    isActive: true,
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    name: 'USB-C Cable',
    description: 'Premium USB-C charging cable 2m',
    price: 19.99,
    category: 'Accessories',
    sku: 'CABLE-001',
    isActive: true,
  },
  {
    id: '44444444-4444-4444-4444-444444444444',
    name: 'Monitor 4K',
    description: '27-inch 4K Ultra HD monitor',
    price: 399.99,
    category: 'Electronics',
    sku: 'MONITOR-001',
    isActive: true,
  },
  {
    id: '55555555-5555-5555-5555-555555555555',
    name: 'Mechanical Keyboard',
    description: 'RGB mechanical gaming keyboard',
    price: 149.99,
    category: 'Accessories',
    sku: 'KEYBOARD-001',
    isActive: true,
  },
];

// A demo customer id used for placing orders from the UI.
export const DEMO_CUSTOMER_ID = '99999999-9999-9999-9999-999999999999';
