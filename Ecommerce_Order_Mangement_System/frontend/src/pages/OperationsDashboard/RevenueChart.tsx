import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { useGetRevenueDailyQuery } from '../../services/api';

export default function RevenueChart() {
  const { data, isLoading } = useGetRevenueDailyQuery(30, { pollingInterval: 10000 });

  const chartData = (data?.records ?? [])
    .map((r) => ({
      date: r.revenueDate,
      revenue: r.totalRevenue,
      orders: r.totalOrders,
    }))
    .reverse();

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h2 className="font-semibold mb-4">Revenue (Last 30 Days)</h2>
      {isLoading ? (
        <p className="text-gray-400">Loading...</p>
      ) : chartData.length === 0 ? (
        <p className="text-gray-400">No revenue data yet.</p>
      ) : (
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" fontSize={12} />
            <YAxis fontSize={12} />
            <Tooltip />
            <Line type="monotone" dataKey="revenue" stroke="#009dd0" strokeWidth={2} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
