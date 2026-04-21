import { Metadata } from "next";
import IncidentDetailClient from "./incident-detail-client";

export const metadata: Metadata = {
    title: "Incident | TraceRoot",
};

export default function IncidentDetailPage() {
    return <IncidentDetailClient />;
}