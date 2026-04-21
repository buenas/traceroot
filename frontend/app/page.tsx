import { IncidentList } from "@/components/incident-list";
import { PageHeader } from "@/components/page-header";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { BarChart3 } from "lucide-react";
import { Metadata } from "next";

export const metadata: Metadata = {
    title: "Incidents | TraceRoot",
    description: "AI-powered reliability platform",
};

export default function Home() {
    return (
        <main className="min-h-screen bg-background">
            <PageHeader
                title="TraceRoot"
                subtitle="AI-powered reliability platform"
                actions={
                    <Button variant="outline" size="sm" asChild>
                        <Link href="/metrics">
                            <BarChart3 className="mr-2 h-4 w-4" />
                            Metrics
                        </Link>
                    </Button>
                }
            />
            <div className="mx-auto max-w-7xl px-8 py-8">
                <IncidentList />
            </div>
        </main>
    );
}