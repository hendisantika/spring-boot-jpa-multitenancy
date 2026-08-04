"use client";

import { useActionState } from "react";

import {
  cancelEmailChange,
  requestEmailChange,
  type EmailChangeState,
} from "@/app/actions/account";
import { PasswordInput } from "@/components/PasswordInput";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function AccountEmailForm({
  email,
  /** An address already asked for and not yet confirmed, if there is one. */
  pendingEmail,
}: {
  email: string;
  pendingEmail: string | null;
}) {
  const [state, action] = useActionState<EmailChangeState, FormData>(requestEmailChange, {});
  const [cancelState, cancel] = useActionState<FormState, FormData>(
    () => cancelEmailChange(),
    {},
  );

  // What was just submitted wins, so the pending notice appears immediately
  // rather than only after the page happens to be reloaded.
  const waitingFor = state.ok ? String(state.values?.email ?? pendingEmail ?? "") : pendingEmail;

  return (
    <div className="space-y-4">
      {waitingFor ? (
        <div className="rounded-xl border border-brand/30 bg-brand/10 p-4">
          <p className="text-sm font-medium text-ink">Waiting for {waitingFor}</p>
          <p className="mt-0.5 text-sm text-ink-muted">
            {/* Said plainly: the point of the wait is that nothing has changed yet. */}
            You still sign in as {email} until that address is confirmed from the link sent to
            it.
          </p>
          {state.confirmUrl ? (
            <a
              href={state.confirmUrl}
              className="mt-3 block truncate font-mono text-xs text-brand hover:underline"
            >
              {state.confirmUrl}
            </a>
          ) : null}
          <form action={cancel} className="mt-3">
            <SubmitButton variant="ghost" pendingLabel="Cancelling…">
              Cancel the change
            </SubmitButton>
          </form>
        </div>
      ) : null}

      {/* Re-keyed once a request goes through, so the fields start empty again. */}
      <form action={action} className="space-y-4" key={waitingFor ?? "none"}>
        {state.error ? <Alert>{state.error}</Alert> : null}
        {cancelState.error ? <Alert>{cancelState.error}</Alert> : null}

        <Field label="New email">
          <Input
            name="email"
            type="email"
            required
            maxLength={255}
            defaultValue={state.values?.email ?? ""}
          />
        </Field>

        <Field
          label="Current password"
          hint="This is the address you sign in with, so it takes your password to change it."
        >
          <PasswordInput name="password" required autoComplete="current-password" />
        </Field>

        <div className="flex items-center justify-end border-t border-line pt-4">
          <SubmitButton pendingLabel="Sending…">
            {waitingFor ? "Send to a different address" : "Send confirmation"}
          </SubmitButton>
        </div>
      </form>
    </div>
  );
}
