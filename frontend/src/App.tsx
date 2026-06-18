import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProductListing from './pages/CustomerPortal/ProductListing';
import Cart from './pages/CustomerPortal/Cart';
import Checkout from './pages/CustomerPortal/Checkout';
import OrderTracking from './pages/CustomerPortal/OrderTracking';
import OrderHistory from './pages/CustomerPortal/OrderHistory';
import OperationsDashboard from './pages/OperationsDashboard';
import InventoryPortal from './pages/InventoryPortal';

export default function App() {
  return (
    <div className="min-h-screen">
      <Navbar />
      <Routes>
        <Route path="/" element={<ProductListing />} />
        <Route path="/cart" element={<Cart />} />
        <Route path="/checkout" element={<Checkout />} />
        <Route path="/track/:orderId" element={<OrderTracking />} />
        <Route path="/orders" element={<OrderHistory />} />
        <Route path="/ops" element={<OperationsDashboard />} />
        <Route path="/inventory" element={<InventoryPortal />} />
      </Routes>
    </div>
  );
}
