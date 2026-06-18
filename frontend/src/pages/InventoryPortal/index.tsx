import StockDashboard from './StockDashboard';
import ProductCRUD from './ProductCRUD';

export default function InventoryPortal() {
  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-4">
      <h1 className="text-2xl font-bold">Inventory Management</h1>
      <StockDashboard />
      <ProductCRUD />
    </div>
  );
}
