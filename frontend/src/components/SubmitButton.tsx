"use client";

import { useFormStatus } from "react-dom";

/**
 * Reads the enclosing form's pending state, so it works for any action without
 * threading a flag through.
 */
export function SubmitButton({
  children,
  pendingLabel,
  variant = "primary",
  className = "",
}: {
  children: React.ReactNode;
  pendingLabel?: string;
  variant?: "primary" | "ghost";
  className?: string;
}) {
  const { pending } = useFormStatus();
  const base =
    "inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium " +
    "transition disabled:cursor-not-allowed disabled:opacity-60";
  const styles =
    variant === "primary"
      ? "bg-brand text-brand-ink hover:opacity-90"
      : "border border-line bg-surface text-ink hover:bg-surface-muted";

  return (
    <button type="submit" disabled={pending} className={`${base} ${styles} ${className}`}>
      {pending && (
        <span
          aria-hidden
          className="size-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
        />
      )}
      {pending ? (pendingLabel ?? "Working…") : children}
    </button>
  );
}
