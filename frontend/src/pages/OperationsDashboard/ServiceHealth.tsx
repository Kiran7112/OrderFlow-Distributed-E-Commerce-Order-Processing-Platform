import { useEffect, useState } from 'react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface ServiceStatus {
  name: string;
  url: string;
  status: 'UP' | 'DOWN' | 'CHECKING';
}

const SERVICES = [
  { name: 'API Gateway', url: `${API_URL}/actuator/health` },
  { name: 'Order Service', url: `${API_URL}/api/orders/health` },
  { name: 'Inventory Service', url: `${API_URL}/api/inventory/health` },
  { name: 'Payment Service', url: `${API_URL}/api/payments/health` },
  { name: 'Shipping Service', url: `${API_URL}/api/shipping/health` },
  { name: 'Analytics Service', url: `${API_URL}/api/analytics/health` },
];

export default function ServiceHealth() {
  const [statuses, setStatuses] = useState<ServiceStatus[]>(
    SERVICES.map((s) => ({ ...s, status: 'CHECKING' as const }))
  );

  useEffect(() => {
    const check = async () => {
      const results = await Promise.all(
        SERVICES.map(async (s) => {
          try {
            const res = await fetch(s.url, { method: 'GET' });
            return { ...s, status: res.ok ? ('UP' as const) : ('DOWN' as const) };
          } catch {
            return { ...s, status: 'DOWN' as const };
          }
        })
      );
      setStatuses(results);
    };
    check();
    const interval = setInterval(check, 10000);
    return () => clearInterval(interval);
  }, []);

  const dot = (status: string) => {
    if (status === 'UP') return 'bg-green-500';
    if (status === 'DOWN') return 'bg-red-500';
    return 'bg-yellow-500';
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h2 className="font-semibold mb-4">Service Health</h2>
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        {statuses.map((s) => (
          <div key={s.name} className="flex items-center gap-2 border rounded-lg p-3">
            <span className={`w-3 h-3 rounded-full ${dot(s.status)}`} />
            <span className="text-sm">{s.name}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
