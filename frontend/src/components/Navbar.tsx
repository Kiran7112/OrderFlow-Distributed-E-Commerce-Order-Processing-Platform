import { Link, useLocation } from 'react-router-dom';
import { useAppSelector } from '../store/hooks';

export default function Navbar() {
  const location = useLocation();
  const cartCount = useAppSelector((s) =>
    s.cart.items.reduce((sum, i) => sum + i.quantity, 0)
  );

  const navItem = (path: string, label: string) => (
    <Link
      to={path}
      className={`px-3 py-2 rounded-md text-sm font-medium ${
        location.pathname === path
          ? 'bg-primary text-white'
          : 'text-gray-700 hover:bg-gray-100'
      }`}
    >
      {label}
    </Link>
  );

  return (
    <nav className="bg-white shadow-sm border-b">
      <div className="max-w-7xl mx-auto px-4 flex items-center justify-between h-16">
        <div className="flex items-center gap-2">
          <span className="text-xl font-bold text-primary">OrderFlow</span>
          <div className="hidden md:flex gap-1 ml-6">
            {navItem('/', 'Shop')}
            {navItem('/orders', 'My Orders')}
            {navItem('/ops', 'Operations')}
            {navItem('/inventory', 'Inventory')}
          </div>
        </div>
        <Link to="/cart" className="relative px-3 py-2 text-gray-700 hover:bg-gray-100 rounded-md">
          Cart
          {cartCount > 0 && (
            <span className="absolute -top-1 -right-1 bg-danger text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
              {cartCount}
            </span>
          )}
        </Link>
      </div>
    </nav>
  );
}
