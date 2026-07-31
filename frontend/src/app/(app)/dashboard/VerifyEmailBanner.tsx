"use client";

import { useActionState } from "react";

import { resendVerification, type ResendState } from "@/app/actions/verification";
import { SubmitButton } from "@/components/SubmitButton";

/**
 * Shown until the address is confirmed. Registering an organization is refused
 * until then, so saying why up front beats a rejection later.
 */
export function VerifyEmailBanner({ email }: { email: string }) {
  const [state, action] = useActionState<ResendState, FormData>(() => resendVerification(), {});

  return (
    <div className="mb-6 rounded-xl border border-brand/30 bg-brand/10 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium text-ink">Confirm {email}</p>
          <p className="mt-0.5 text-sm text-ink-muted">
            {state.ok
              ? state.message
              : (state.error ?? "Registering an organization needs a confirmed address.")}
          </p>
        </div>
        {!state.ok ? (
          <form action={action}>
            <SubmitButton variant="ghost" pendingLabel="Sending…">
              Resend link
            </SubmitButton>
          </form>
        ) : null}
      </div>

      {state.verifyUrl ? (
        <a
          href={state.verifyUrl}
          className="mt-3 block truncate font-mono text-xs text-brand hover:underline"
        >
          {state.verifyUrl}
        </a>
      ) : null}
    </div>
  );
}
