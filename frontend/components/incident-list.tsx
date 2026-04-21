"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { formatDistanceToNow } from "date-fns";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useRouter } from "next/navigation";
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

type StatusFilter = "ALL" | "ACTIVE" | "RESOLVED";

async function fetchIncidents(): Promise<Incident[]> {
    const res = await fetch("/api/incidents");
    if (!res.ok) {
        throw new Error(`Failed to fetch incidents: ${res.status}`);
    }
    return res.json();
}

export function IncidentList() {
    const router = useRouter();
    const [filter, setFilter] = useState<StatusFilter>("ALL");

    const { data, isLoading, error } = useQuery({
        queryKey: ["incidents"],
        queryFn: fetchIncidents,
    });

    if (isLoading) {
        return <p className="text-muted-foreground">Loading incidents...</p>;
    }

    if (error) {
        return (
            <div className="rounded-md border border-destructive/50 bg-destructive/5 p-6">
                <h3 className="mb-2 text-sm font-semibold text-destructive">
                    Unable to load incidents
                </h3>
                <p className="text-sm text-muted-foreground">
                    {error instanceof Error
                        ? error.message
                        : "Something went wrong. Please refresh the page."}
                </p>
            </div>
        );
    }

    if (!data) return null;

    const filtered =
        filter === "ALL" ? data : data.filter((i) => i.incidentStatus === filter);

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-lg font-semibold">Incidents</h2>
                    <p className="text-sm text-muted-foreground">
                        {filtered.length} {filter === "ALL" ? "total" : filter.toLowerCase()}
                    </p>
                </div>
                <div className="flex gap-2">
                    <Button
                        variant={filter === "ALL" ? "default" : "outline"}
                        size="sm"
                        onClick={() => setFilter("ALL")}
                    >
                        All
                    </Button>
                    <Button
                        variant={filter === "ACTIVE" ? "default" : "outline"}
                        size="sm"
                        onClick={() => setFilter("ACTIVE")}
                    >
                        Active
                    </Button>
                    <Button
                        variant={filter === "RESOLVED" ? "default" : "outline"}
                        size="sm"
                        onClick={() => setFilter("RESOLVED")}
                    >
                        Resolved
                    </Button>
                </div>
            </div>

            {filtered.length === 0 ? (
                <div className="rounded-md border p-12 text-center">
                    <div className="mx-auto max-w-sm space-y-2">
                        <h3 className="text-sm font-semibold">
                            No {filter === "ALL" ? "" : filter.toLowerCase()} incidents
                        </h3>
                        <p className="text-sm text-muted-foreground">
                            {filter === "ALL"
                                ? "When failures are detected, incidents will appear here."
                                : filter === "ACTIVE"
                                    ? "Active incidents will appear here when detection fires."
                                    : "Resolved incidents will appear here once marked."}
                        </p>
                    </div>
                </div>
            ) : (
                <div className="rounded-md border">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-[100px]">Status</TableHead>
                                <TableHead>Title</TableHead>
                                <TableHead>Service</TableHead>
                                <TableHead className="text-right">Events</TableHead>
                                <TableHead>Last seen</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {filtered.map((incident) => (
                                <TableRow
                                    key={incident.id}
                                    className="cursor-pointer"
                                    onClick={() => router.push(`/incidents/${incident.id}`)}>
                                    <TableCell>
                                        <Badge
                                            variant={
                                                incident.incidentStatus === "ACTIVE"
                                                    ? "destructive"
                                                    : "secondary"
                                            }
                                        >
                                            {incident.incidentStatus}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className="font-medium">{incident.title}</TableCell>
                                    <TableCell className="text-muted-foreground">
                                        {incident.serviceName}
                                    </TableCell>
                                    <TableCell className="text-right">
                                        {incident.eventCount}
                                    </TableCell>
                                    <TableCell className="text-muted-foreground">
                                        {formatDistanceToNow(new Date(incident.lastSeenAt), {
                                            addSuffix: true,
                                        })}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </div>
            )}
        </div>
    );
}