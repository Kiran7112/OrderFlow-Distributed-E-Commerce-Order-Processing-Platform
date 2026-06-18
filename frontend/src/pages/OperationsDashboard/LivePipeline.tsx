import { useGetOrderSummaryQuery } from '../../services/api';

export default function LivePipeline() {
  const { data, isLoading } = useGetOrderSummaryQuery(24, { pollingInterval: 5000 });

  const totals = (data?.metrics ?? []).reduce(
    (acc, m) => ({
      total: acc.total + m.totalOrders,
      confirmed: acc.confirmed + m.confirmedOrders,
      shipped: acc.shipped + m.shippedOrders,
      delivered: acc.delivered + m.deliveredOrders,
      cancelled: acc.cancelled + m.cancelledOrders,
    }),
    { total: 0, confirmed: 0, shipped: 0, delivered: 0, cancelled: 0 }
  );

  const stages = [
    { label: 'Placed', value: totals.total, color: 'bg-blue-500' },
    { label: 'Confirmed', value: totals.confirmed, color: 'bg-indigo-500' },
    { label: 'Shipped', value: totals.shipped, color: 'bg-purple-500' },
    { label: 'Delivered', value: totals.delivered, color: 'bg-green-500' },
    { label: 'Cancelled', value: totals.cancelled, color: 'bg-red-500' },
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h2 className="font-semibold mb-4">Live Order Pipeline (24h)</h2>
      {isLoading ? (
        <p className="text-gray-400">Loading...</p>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          {stages.map((s) => (
            <div key={s.label} className="text-center">
              <div className={`${s.color} text-white rounded-lg py-4 text-2xl font-bold`}>
                {s.value}
              </div>
              <p className="text-sm text-gray-600 mt-1">{s.label}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
