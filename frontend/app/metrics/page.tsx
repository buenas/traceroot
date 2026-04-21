import { Metadata } from "next";
import MetricsClient from "./metrics-client";

export const metadata: Metadata = {
    title: "Metrics | TraceRoot",
    description: "Platform-wide incident metrics",
};

export default function MetricsPage() {
    return <MetricsClient />;
}