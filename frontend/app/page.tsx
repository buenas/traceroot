import { IncidentList } from "@/components/incident-list";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { BarChart3 } from "lucide-react";

export default function Home() {
    return (
        <main className="min-h-screen bg-background">
            <div className="border-b">
                <div className="mx-auto max-w-7xl px-8 py-6 flex items-start justify-between">
                    <div>
                        <h1 className="text-2xl font-bold">TraceRoot</h1>
                        <p className="text-sm text-muted-foreground">
                            AI-powered reliability platform
                        </p>
                    </div>
                    <Button variant="outline" size="sm" asChild>
                        <Link href="/metrics">
                            <BarChart3 className="mr-2 h-4 w-4" />
                            Metrics
                        </Link>
                    </Button>
                </div>
            </div>
            <div className="mx-auto max-w-7xl px-8 py-8">
                <IncidentList />
            </div>
        </main>
    );
}