import { Link } from 'react-router-dom';
import { useGetOrdersByCustomerQuery } from '../../services/api';
import { DEMO_CUSTOMER_ID } from '../../data/mockProducts';
import StatusBadge from '../../components/StatusBadge';

export default function OrderHistory() {
  const { data: orders, isLoading } = useGetOrdersByCustomerQuery(DEMO_CUSTOMER_ID, {
    pollingInterval: 5000,
  });

  if (isLoading) return <div className="text-center py-12 text-gray-500">Loading orders...</div>;

  return (
    <div className="max-w-4xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold mb-4">Order History</h1>
      {!orders || orders.length === 0 ? (
        <div className="text-center py-12 text-gray-500">No orders yet.</div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm divide-y">
          {orders.map((order) => (
            <Link
              key={order.id}
              to={`/track/${order.id}`}
              className="p-4 flex items-center justify-between hover:bg-gray-50"
            >
              <div>
                <p className="font-mono text-sm text-gray-600">{order.id.slice(0, 8)}...</p>
                <p className="text-sm text-gray-500">
                  {new Date(order.createdAt).toLocaleString()}
                </p>
              </div>
              <div className="flex items-center gap-4">
                <span className="font-bold">${order.totalAmount?.toFixed(2)}</span>
                <StatusBadge status={order.status} />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
