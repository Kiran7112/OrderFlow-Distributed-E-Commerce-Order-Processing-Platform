import { useParams } from 'react-router-dom';
import { useGetOrderQuery, useGetShipmentByOrderQuery } from '../../services/api';
import StatusBadge from '../../components/StatusBadge';

const TIMELINE: { status: string; label: string }[] = [
  { status: 'PLACED', label: 'Order Placed' },
  { status: 'CONFIRMED', label: 'Confirmed (Stock Reserved)' },
  { status: 'SHIPPED', label: 'Shipped' },
  { status: 'DELIVERED', label: 'Delivered' },
];

const ORDER_INDEX: Record<string, number> = {
  PLACED: 0,
  CONFIRMED: 1,
  SHIPPED: 2,
  DELIVERED: 3,
};

export default function OrderTracking() {
  const { orderId } = useParams<{ orderId: string }>();
  // Poll every 3 seconds for live status updates.
  const { data: order, isLoading } = useGetOrderQuery(orderId!, {
    pollingInterval: 3000,
  });
  const { data: shipment } = useGetShipmentByOrderQuery(orderId!, {
    pollingInterval: 5000,
    skip: !order || (order.status !== 'SHIPPED' && order.status !== 'DELIVERED'),
  });

  if (isLoading) return <div className="text-center py-12 text-gray-500">Loading order...</div>;
  if (!order) return <div className="text-center py-12 text-gray-500">Order not found.</div>;

  const currentIndex = ORDER_INDEX[order.status] ?? -1;
  const isFailed = order.status === 'CANCELLED' || order.status === 'PAYMENT_FAILED';

  return (
    <div className="max-w-3xl mx-auto px-4 py-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">Order Tracking</h1>
        <StatusBadge status={order.status} />
      </div>

      <div className="bg-white rounded-lg shadow-sm p-4 mb-4">
        <p className="text-sm text-gray-500">Order ID</p>
        <p className="font-mono text-sm">{order.id}</p>
        <p className="text-sm text-gray-500 mt-2">Total</p>
        <p className="font-bold text-lg">${order.totalAmount?.toFixed(2)} {order.currency}</p>
      </div>

      {isFailed ? (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-800">
          {order.status === 'CANCELLED'
            ? 'Order was cancelled (out of stock).'
            : 'Payment failed. Please try again.'}
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="space-y-6">
            {TIMELINE.map((step, idx) => {
              const done = idx <= currentIndex;
              return (
                <div key={step.status} className="flex items-center gap-4">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
                      done ? 'bg-primary text-white' : 'bg-gray-200 text-gray-400'
                    }`}
                  >
                    {done ? '✓' : idx + 1}
                  </div>
                  <span className={done ? 'font-semibold' : 'text-gray-400'}>{step.label}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {shipment && (
        <div className="bg-white rounded-lg shadow-sm p-4 mt-4">
          <h2 className="font-semibold mb-2">Shipment Details</h2>
          <p className="text-sm">Tracking: <span className="font-mono">{shipment.trackingNumber}</span></p>
          <p className="text-sm">Carrier: {shipment.carrier}</p>
          <p className="text-sm">Estimated Delivery: {shipment.estimatedDelivery}</p>
        </div>
      )}
    </div>
  );
}
