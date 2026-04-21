"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";

interface PageHeaderProps {
    title: string;
    subtitle?: string;
    backHref?: string;
    backLabel?: string;
    actions?: React.ReactNode;
}

export function PageHeader({
                               title,
                               subtitle,
                               backHref,
                               backLabel = "Back",
                               actions,
                           }: PageHeaderProps) {
    return (
        <div className="border-b">
            <div className="mx-auto max-w-7xl px-8 py-6">
                {backHref && (
                    <Link
                        href={backHref}
                        className="mb-4 inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
                    >
                        <ArrowLeft className="mr-1 h-4 w-4" />
                        {backLabel}
                    </Link>
                )}
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <h1 className="text-2xl font-bold">{title}</h1>
                        {subtitle && (
                            <p className="text-sm text-muted-foreground">{subtitle}</p>
                        )}
                    </div>
                    {actions && <div>{actions}</div>}
                </div>
            </div>
        </div>
    );
}