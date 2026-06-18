import { useGetLowStockQuery } from '../../services/api';

export default function StockDashboard() {
  const { data: lowStock, isLoading } = useGetLowStockQuery(undefined, {
    pollingInterval: 5000,
  });

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h2 className="font-semibold mb-4">Low Stock Alerts</h2>
      {isLoading ? (
        <p className="text-gray-400">Loading...</p>
      ) : !lowStock || lowStock.length === 0 ? (
        <p className="text-success">All products are well stocked.</p>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-gray-500 border-b">
              <th className="py-2">Product ID</th>
              <th className="py-2 text-right">Available</th>
              <th className="py-2 text-right">Reserved</th>
              <th className="py-2 text-right">Reorder Level</th>
            </tr>
          </thead>
          <tbody>
            {lowStock.map((s) => (
              <tr key={s.productId} className="border-b">
                <td className="py-2 font-mono text-xs">{s.productId.slice(0, 8)}...</td>
                <td className="py-2 text-right text-danger font-semibold">{s.availableQty}</td>
                <td className="py-2 text-right">{s.reservedQty}</td>
                <td className="py-2 text-right">{s.reorderLevel}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
