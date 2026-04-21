"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { ArrowLeft, Activity, CheckCircle2, AlertCircle, Clock } from "lucide-react";
import {
    Bar,
    BarChart,
    CartesianGrid,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

interface IncidentMetrics {
    activeIncidents: number;
    resolvedIncidents: number;
    totalIncidents: number;
    incidentsCreatedLast24Hours: number;
    incidentsResolvedLast24Hours: number;
    averageResolutionMinutes: number;
}

interface ServiceMetrics {
    serviceName: string;
    incidentCount: number;
    activeCount: number;
    resolvedCount: number;
}

interface TopPattern {
    fingerPrint: string;
    title: string;
    incidentCount: number;
}

async function fetchMetrics(): Promise<IncidentMetrics> {
    const res = await fetch("/api/metrics/incidents");
    if (!res.ok) throw new Error("Failed to fetch metrics");
    return res.json();
}

async function fetchServiceMetrics(): Promise<ServiceMetrics[]> {
    const res = await fetch("/api/metrics/incidents/services");
    if (!res.ok) throw new Error("Failed to fetch service metrics");
    return res.json();
}

async function fetchTopPatterns(): Promise<TopPattern[]> {
    const res = await fetch("/api/metrics/incidents/top-patterns");
    if (!res.ok) throw new Error("Failed to fetch top patterns");
    return res.json();
}

export default function MetricsPage() {
    const metricsQuery = useQuery({
        queryKey: ["metrics"],
        queryFn: fetchMetrics,
    });

    const serviceQuery = useQuery({
        queryKey: ["metrics-services"],
        queryFn: fetchServiceMetrics,
    });

    const patternsQuery = useQuery({
        queryKey: ["metrics-patterns"],
        queryFn: fetchTopPatterns,
    });

    return (
        <main className="min-h-screen bg-background">
            <div className="border-b">
                <div className="mx-auto max-w-7xl px-8 py-6">
                    <Link
                        href="/"
                        className="mb-4 inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
                    >
                        <ArrowLeft className="mr-1 h-4 w-4" />
                        Back to incidents
                    </Link>
                    <h1 className="text-2xl font-bold">Metrics</h1>
                    <p className="text-sm text-muted-foreground">
                        Platform-wide incident health
                    </p>
                </div>
            </div>

            <div className="mx-auto max-w-7xl space-y-6 px-8 py-8">
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <MetricCard
                        title="Active incidents"
                        value={metricsQuery.data?.activeIncidents}
                        loading={metricsQuery.isLoading}
                        icon={<AlertCircle className="h-4 w-4 text-red-500" />}
                    />
                    <MetricCard
                        title="Resolved incidents"
                        value={metricsQuery.data?.resolvedIncidents}
                        loading={metricsQuery.isLoading}
                        icon={<CheckCircle2 className="h-4 w-4 text-green-500" />}
                    />
                    <MetricCard
                        title="Created (24h)"
                        value={metricsQuery.data?.incidentsCreatedLast24Hours}
                        loading={metricsQuery.isLoading}
                        icon={<Activity className="h-4 w-4 text-blue-500" />}
                    />
                    <MetricCard
                        title="Avg resolution"
                        value={
                            metricsQuery.data?.averageResolutionMinutes !== undefined
                                ? `${metricsQuery.data.averageResolutionMinutes.toFixed(1)}m`
                                : undefined
                        }
                        loading={metricsQuery.isLoading}
                        icon={<Clock className="h-4 w-4 text-purple-500" />}
                    />
                </div>

                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">Incidents by service</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {serviceQuery.isLoading ? (
                            <Skeleton className="h-64 w-full" />
                        ) : serviceQuery.data && serviceQuery.data.length > 0 ? (
                            <div className="h-64">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={serviceQuery.data}>
                                        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                                        <XAxis
                                            dataKey="serviceName"
                                            className="text-xs"
                                            tick={{ fill: "currentColor" }}
                                        />
                                        <YAxis
                                            className="text-xs"
                                            tick={{ fill: "currentColor" }}
                                        />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: "hsl(var(--background))",
                                                border: "1px solid hsl(var(--border))",
                                                borderRadius: "6px",
                                            }}
                                        />
                                        <Bar dataKey="activeCount" fill="#ef4444" name="Active" />
                                        <Bar dataKey="resolvedCount" fill="#94a3b8" name="Resolved" />
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        ) : (
                            <p className="text-sm text-muted-foreground">No service data yet.</p>
                        )}
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">Top recurring patterns</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {patternsQuery.isLoading ? (
                            <div className="space-y-2">
                                <Skeleton className="h-8 w-full" />
                                <Skeleton className="h-8 w-full" />
                                <Skeleton className="h-8 w-full" />
                            </div>
                        ) : patternsQuery.data && patternsQuery.data.length > 0 ? (
                            <div className="space-y-2">
                                {patternsQuery.data.map((p) => (
                                    <div
                                        key={p.fingerPrint}
                                        className="flex items-center justify-between border-b pb-2 last:border-0 last:pb-0"
                                    >
                                        <div className="min-w-0 flex-1">
                                            <p className="truncate text-sm font-medium">{p.title}</p>
                                            <p className="truncate text-xs text-muted-foreground font-mono">
                                                {p.fingerPrint}
                                            </p>
                                        </div>
                                        <div className="ml-4 text-sm font-medium">
                                            {p.incidentCount}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-sm text-muted-foreground">No patterns yet.</p>
                        )}
                    </CardContent>
                </Card>
            </div>
        </main>
    );
}

function MetricCard({
                        title,
                        value,
                        loading,
                        icon,
                    }: {
    title: string;
    value: number | string | undefined;
    loading: boolean;
    icon: React.ReactNode;
}) {
    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                    {title}
                </CardTitle>
                {icon}
            </CardHeader>
            <CardContent>
                {loading ? (
                    <Skeleton className="h-8 w-16" />
                ) : (
                    <div className="text-2xl font-bold">{value ?? "—"}</div>
                )}
            </CardContent>
        </Card>
    );
}