import LivePipeline from './LivePipeline';
import RevenueChart from './RevenueChart';
import FailureMonitor from './FailureMonitor';
import KafkaLagPanel from './KafkaLagPanel';
import ServiceHealth from './ServiceHealth';

export default function OperationsDashboard() {
  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-4">
      <h1 className="text-2xl font-bold">Operations Dashboard</h1>
      <LivePipeline />
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <RevenueChart />
        <FailureMonitor />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <KafkaLagPanel />
        <ServiceHealth />
      </div>
    </div>
  );
}
