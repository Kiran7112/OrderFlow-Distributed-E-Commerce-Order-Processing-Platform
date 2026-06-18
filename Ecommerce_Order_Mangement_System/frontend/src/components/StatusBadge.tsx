interface Props {
  status: string;
}

const STATUS_COLORS: Record<string, string> = {
  PLACED: 'bg-blue-100 text-blue-800',
  CONFIRMED: 'bg-indigo-100 text-indigo-800',
  SHIPPED: 'bg-purple-100 text-purple-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
  PAYMENT_FAILED: 'bg-red-100 text-red-800',
  SUCCESS: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  PENDING: 'bg-yellow-100 text-yellow-800',
  CREATED: 'bg-blue-100 text-blue-800',
  IN_TRANSIT: 'bg-purple-100 text-purple-800',
};

export default function StatusBadge({ status }: Props) {
  const color = STATUS_COLORS[status] || 'bg-gray-100 text-gray-800';
  return <span className={`badge ${color}`}>{status}</span>;
}
