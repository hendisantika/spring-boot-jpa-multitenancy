import type { ComponentProps, ReactNode } from "react";

/** Exported so PasswordInput can wear the same field without copying it. */
export const FIELD =
  "w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink outline-none " +
  "transition placeholder:text-ink-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/20";

export function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-ink">{label}</span>
      {children}
      {hint ? <span className="mt-1 block text-xs text-ink-muted">{hint}</span> : null}
    </label>
  );
}

export function Input(props: ComponentProps<"input">) {
  return <input {...props} className={`${FIELD} ${props.className ?? ""}`} />;
}

export function Select(props: ComponentProps<"select">) {
  return <select {...props} className={`${FIELD} ${props.className ?? ""}`} />;
}

export function Alert({ tone = "danger", children }: { tone?: "danger" | "info"; children: ReactNode }) {
  const styles =
    tone === "danger"
      ? "border-danger/30 bg-danger/10 text-danger"
      : "border-brand/30 bg-brand/10 text-ink";
  return (
    <p role="alert" className={`rounded-lg border px-3 py-2 text-sm ${styles}`}>
      {children}
    </p>
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-line bg-surface shadow-sm ${className}`}>{children}</div>
  );
}

export function Badge({ children, tone = "muted" }: { children: ReactNode; tone?: "muted" | "brand" }) {
  const styles =
    tone === "brand" ? "bg-brand/12 text-brand" : "bg-surface-muted text-ink-muted";
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${styles}`}>
      {children}
    </span>
  );
}

export function PageHeading({ title, description }: { title: string; description?: string }) {
  return (
    <div className="mb-6">
      <h1 className="text-2xl font-semibold tracking-tight text-ink">{title}</h1>
      {description ? <p className="mt-1 text-sm text-ink-muted">{description}</p> : null}
    </div>
  );
}
