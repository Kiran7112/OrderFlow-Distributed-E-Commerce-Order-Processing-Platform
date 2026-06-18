import { useGetConsumerLagQuery } from '../../services/api';

export default function KafkaLagPanel() {
  const { data, isLoading } = useGetConsumerLagQuery(undefined, { pollingInterval: 5000 });

  const metrics = data?.metrics ? Object.entries(data.metrics) : [];

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h2 className="font-semibold mb-4">Kafka Consumer Lag</h2>
      {isLoading ? (
        <p className="text-gray-400">Loading...</p>
      ) : (
        <>
          <div className="mb-3">
            <span className="text-sm text-gray-500">Total Lag: </span>
            <span
              className={`font-bold ${
                (data?.totalLag ?? 0) > 100 ? 'text-danger' : 'text-success'
              }`}
            >
              {data?.totalLag ?? 0}
            </span>
          </div>
          {metrics.length === 0 ? (
            <p className="text-gray-400 text-sm">No lag data available.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b">
                  <th className="py-1">Topic-Partition</th>
                  <th className="py-1 text-right">Lag</th>
                </tr>
              </thead>
              <tbody>
                {metrics.map(([key, m]) => (
                  <tr key={key} className="border-b">
                    <td className="py-1">{key}</td>
                    <td className={`py-1 text-right ${m.lag > 50 ? 'text-danger' : ''}`}>
                      {m.lag}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}
