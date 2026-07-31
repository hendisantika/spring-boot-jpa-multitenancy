"use client";

import { useActionState } from "react";

import { requestPasswordReset, type ForgotState } from "@/app/actions/password";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";

export function ForgotPasswordForm() {
  const [state, action] = useActionState<ForgotState, FormData>(requestPasswordReset, {});

  return (
    <form action={action} className="space-y-4">
      {state.error ? <Alert>{state.error}</Alert> : null}

      {/* The same message either way: whether the address has an account is not
          something this page should reveal. */}
      {state.ok ? <Alert tone="info">{state.message}</Alert> : null}

      {state.ok && state.resetUrl ? (
        <div className="rounded-lg border border-brand/30 bg-brand/10 p-3">
          <p className="text-xs text-ink-muted">
            Email delivery is not configured, so the link is shown here instead.
          </p>
          <a
            href={state.resetUrl}
            className="mt-2 block truncate font-mono text-xs text-brand hover:underline"
          >
            {state.resetUrl}
          </a>
        </div>
      ) : null}

      <Field label="Email">
        <Input
          name="email"
          type="email"
          required
          autoComplete="email"
          placeholder="you@example.com"
          defaultValue={state.values?.email ?? ""}
        />
      </Field>

      <SubmitButton className="w-full" pendingLabel="Sending…">
        Send reset link
      </SubmitButton>
    </form>
  );
}
