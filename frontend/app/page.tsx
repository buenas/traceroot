import { IncidentList } from "@/components/incident-list";

export default function Home() {
  return (
      <main className="min-h-screen bg-background">
        <div className="border-b">
          <div className="mx-auto max-w-7xl px-8 py-6">
            <h1 className="text-2xl font-bold">TraceRoot</h1>
            <p className="text-sm text-muted-foreground">
              AI-powered reliability platform
            </p>
          </div>
        </div>
        <div className="mx-auto max-w-7xl px-8 py-8">
          <IncidentList />
        </div>
      </main>
  );
}