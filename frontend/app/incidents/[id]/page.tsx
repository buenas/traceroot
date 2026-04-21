"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { format } from "date-fns";
import { ArrowLeft, CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

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

interface IncidentSummary {
    incidentId: string;
    summary: string;
    possibleCause: string;
    recommendedChecks: string[];
    generatedAt: string;
}

interface LogResponse {
    id: string;
    level: string;
    serviceName: string;
    message: string;
    timestamp: string;
    environment: string;
    traceId: string;
    endpoint: string;
    exceptionType: string;
    version: string;
    createdAt: string;
}

async function fetchIncident(id: string): Promise<Incident> {
    const res = await fetch(`/api/incidents/${id}`);
    if (!res.ok) throw new Error(`Failed to fetch incident: ${res.status}`);
    return res.json();
}

async function fetchSummary(id: string): Promise<IncidentSummary> {
    const res = await fetch(`/api/incidents/${id}/summary`);
    if (!res.ok) throw new Error(`Failed to fetch summary: ${res.status}`);
    return res.json();
}

async function fetchLogs(id: string): Promise<LogResponse[]> {
    const res = await fetch(`/api/incidents/${id}/logs`);
    if (!res.ok) throw new Error(`Failed to fetch logs: ${res.status}`);
    return res.json();
}

async function resolveIncident(id: string): Promise<Incident> {
    const res = await fetch(`/api/incidents/${id}/resolve`, { method: "POST" });
    if (!res.ok) throw new Error(`Failed to resolve incident: ${res.status}`);
    return res.json();
}

export default function IncidentDetailPage() {
    const params = useParams();
    const router = useRouter();
    const queryClient = useQueryClient();
    const id = params.id as string;

    const incidentQuery = useQuery({
        queryKey: ["incident", id],
        queryFn: () => fetchIncident(id),
    });

    const summaryQuery = useQuery({
        queryKey: ["incident-summary", id],
        queryFn: () => fetchSummary(id),
    });

    const logsQuery = useQuery({
        queryKey: ["incident-logs", id],
        queryFn: () => fetchLogs(id),
    });

    const resolveMutation = useMutation({
        mutationFn: () => resolveIncident(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["incident", id] });
            queryClient.invalidateQueries({ queryKey: ["incidents"] });
        },
    });

    const incident = incidentQuery.data;

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

                    {incidentQuery.isLoading ? (
                        <Skeleton className="h-8 w-96" />
                    ) : incident ? (
                        <div className="flex items-start justify-between gap-4">
                            <div className="space-y-2">
                                <div className="flex items-center gap-3">
                                    <Badge
                                        variant={
                                            incident.incidentStatus === "ACTIVE"
                                                ? "destructive"
                                                : "secondary"
                                        }
                                    >
                                        {incident.incidentStatus}
                                    </Badge>
                                    <span className="text-sm text-muted-foreground">
                    {incident.eventCount} events
                  </span>
                                </div>
                                <h1 className="text-2xl font-bold">{incident.title}</h1>
                                <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                  <span>
                    <span className="font-medium">Service:</span>{" "}
                      {incident.serviceName}
                  </span>
                                    <span>
                    <span className="font-medium">Exception:</span>{" "}
                                        {incident.exceptionType}
                  </span>
                                    <span>
                    <span className="font-medium">Endpoint:</span>{" "}
                                        {incident.endpoint}
                  </span>
                                </div>
                                <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                  <span>
                    First seen:{" "}
                      {format(new Date(incident.firstSeenAt), "MMM d, yyyy h:mm a")}
                  </span>
                                    <span>
                    Last seen:{" "}
                                        {format(new Date(incident.lastSeenAt), "MMM d, yyyy h:mm a")}
                  </span>
                                </div>
                            </div>

                            {incident.incidentStatus === "ACTIVE" && (
                                <Button
                                    onClick={() => resolveMutation.mutate()}
                                    disabled={resolveMutation.isPending}
                                >
                                    <CheckCircle2 className="mr-2 h-4 w-4" />
                                    {resolveMutation.isPending ? "Resolving..." : "Mark resolved"}
                                </Button>
                            )}
                        </div>
                    ) : (
                        <div className="text-red-600">Incident not found.</div>
                    )}
                </div>
            </div>

            <div className="mx-auto max-w-7xl space-y-6 px-8 py-8">
                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">AI Summary</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {summaryQuery.isLoading ? (
                            <div className="space-y-2">
                                <Skeleton className="h-4 w-full" />
                                <Skeleton className="h-4 w-5/6" />
                                <Skeleton className="h-4 w-4/6" />
                            </div>
                        ) : summaryQuery.data ? (
                            <>
                                <div>
                                    <h3 className="text-sm font-medium mb-1">Summary</h3>
                                    <p className="text-sm text-muted-foreground">
                                        {summaryQuery.data.summary}
                                    </p>
                                </div>
                                <Separator />
                                <div>
                                    <h3 className="text-sm font-medium mb-1">Probable cause</h3>
                                    <p className="text-sm text-muted-foreground">
                                        {summaryQuery.data.possibleCause}
                                    </p>
                                </div>
                                <Separator />
                                <div>
                                    <h3 className="text-sm font-medium mb-2">Recommended checks</h3>
                                    <ul className="space-y-1">
                                        {summaryQuery.data.recommendedChecks.map((check, i) => (
                                            <li key={i} className="text-sm text-muted-foreground">
                                                • {check}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            </>
                        ) : (
                            <p className="text-sm text-muted-foreground">
                                Summary unavailable.
                            </p>
                        )}
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">
                            Matching logs{" "}
                            {logsQuery.data && (
                                <span className="text-sm font-normal text-muted-foreground">
                  ({logsQuery.data.length})
                </span>
                            )}
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        {logsQuery.isLoading ? (
                            <div className="space-y-2">
                                <Skeleton className="h-8 w-full" />
                                <Skeleton className="h-8 w-full" />
                                <Skeleton className="h-8 w-full" />
                            </div>
                        ) : logsQuery.data && logsQuery.data.length > 0 ? (
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="w-[180px]">Timestamp</TableHead>
                                        <TableHead className="w-[80px]">Level</TableHead>
                                        <TableHead>Message</TableHead>
                                        <TableHead className="w-[200px]">Trace ID</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {logsQuery.data.map((log) => (
                                        <TableRow key={log.id}>
                                            <TableCell className="text-xs text-muted-foreground">
                                                {format(new Date(log.timestamp), "MMM d, HH:mm:ss")}
                                            </TableCell>
                                            <TableCell>
                                                <Badge variant="outline" className="text-xs">
                                                    {log.level}
                                                </Badge>
                                            </TableCell>
                                            <TableCell className="text-sm">{log.message}</TableCell>
                                            <TableCell className="text-xs text-muted-foreground font-mono">
                                                {log.traceId}
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        ) : (
                            <p className="text-sm text-muted-foreground">No logs found.</p>
                        )}
                    </CardContent>
                </Card>
            </div>
        </main>
    );
}