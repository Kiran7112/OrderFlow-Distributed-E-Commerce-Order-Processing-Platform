import { useGetFailuresQuery } from '../../services/api';

export default function FailureMonitor() {
  const { data, isLoading } = useGetFailuresQuery(undefined, { pollingInterval: 5000 });

  const cards = [
    { label: 'Total Failures (24h)', value: data?.totalFailures ?? 0, color: 'text-gray-900' },
    { label: 'Payment Failures', value: data?.paymentFailures ?? 0, color: 'text-danger' },
    { label: 'Inventory Shortages', value: data?.inventoryFailures ?? 0, color: 'text-warning' },
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h2 className="font-semibold mb-4">Failure Rate Monitor</h2>
      {isLoading ? (
        <p className="text-gray-400">Loading...</p>
      ) : (
        <div className="grid grid-cols-3 gap-3">
          {cards.map((c) => (
            <div key={c.label} className="text-center border rounded-lg p-3">
              <div className={`text-3xl font-bold ${c.color}`}>{c.value}</div>
              <p className="text-xs text-gray-500 mt-1">{c.label}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
