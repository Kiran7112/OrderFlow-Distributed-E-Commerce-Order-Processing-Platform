import { useAppDispatch } from '../../store/hooks';
import { addToCart } from '../../store/cartSlice';
import { MOCK_PRODUCTS } from '../../data/mockProducts';
import { useGetStockQuery } from '../../services/api';
import type { Product } from '../../types';

function ProductCard({ product }: { product: Product }) {
  const dispatch = useAppDispatch();
  const { data: stock } = useGetStockQuery(product.id);

  const stockBadge = () => {
    if (!stock) return null;
    if (stock.availableQty === 0) return <span className="badge bg-red-100 text-red-800">Out of stock</span>;
    if (stock.availableQty <= 5)
      return <span className="badge bg-yellow-100 text-yellow-800">Only {stock.availableQty} left</span>;
    return <span className="badge bg-green-100 text-green-800">In stock</span>;
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-4 flex flex-col">
      <div className="h-32 bg-gray-100 rounded-md mb-3 flex items-center justify-center text-gray-400">
        {product.category}
      </div>
      <h3 className="font-semibold text-gray-900">{product.name}</h3>
      <p className="text-sm text-gray-500 flex-1">{product.description}</p>
      <div className="mt-2 flex items-center justify-between">
        <span className="text-lg font-bold text-primary">${product.price.toFixed(2)}</span>
        {stockBadge()}
      </div>
      <button
        onClick={() => dispatch(addToCart(product))}
        disabled={stock?.availableQty === 0}
        className="mt-3 bg-primary text-white py-2 rounded-md hover:bg-primary-dark disabled:bg-gray-300"
      >
        Add to Cart
      </button>
    </div>
  );
}

export default function ProductListing() {
  return (
    <div className="max-w-7xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold mb-4">Products</h1>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {MOCK_PRODUCTS.map((p) => (
          <ProductCard key={p.id} product={p} />
        ))}
      </div>
    </div>
  );
}
