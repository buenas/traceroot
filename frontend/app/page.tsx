"use client";

import { useQuery } from "@tanstack/react-query";

interface Incident {
  id: string;
  title: string;
  serviceName: string;
  exceptionType: string;
  endpoint: string;
  incidentStatus: "ACTIVE" | "RESOLVED";
  eventCount: number;
  firstSeenAt: string;
  lastSeenAt: string;
}

async function fetchIncidents(): Promise<Incident[]> {
  const res = await fetch("/api/incidents");
  if (!res.ok) {
    throw new Error(`Failed to fetch incidents: ${res.status}`);
  }
  return res.json();
}

export default function Home() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["incidents"],
    queryFn: fetchIncidents,
  });

  return (
      <main className="min-h-screen p-8">
        <h1 className="text-3xl font-bold mb-6">TraceRoot</h1>

        {isLoading && <p className="text-gray-500">Loading incidents...</p>}

        {error && (
            <div className="text-red-600">
              Error: {error instanceof Error ? error.message : "Unknown error"}
            </div>
        )}

        {data && (
            <div>
              <p className="text-gray-600 mb-4">
                Found {data.length} incident{data.length === 1 ? "" : "s"}
              </p>
              <pre className="bg-gray-100 p-4 rounded text-sm overflow-auto">
            {JSON.stringify(data, null, 2)}
          </pre>
            </div>
        )}
      </main>
  );
}