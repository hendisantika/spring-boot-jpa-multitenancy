"use client";

import { useActionState } from "react";

import { acceptInvitation } from "@/app/actions/invitations";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function AcceptForm({
  token,
  slug,
  email,
  needsPassword,
}: {
  token: string;
  slug: string;
  email: string;
  needsPassword: boolean;
}) {
  const [state, action] = useActionState<FormState, FormData>(acceptInvitation, {});

  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="token" value={token} />
      <input type="hidden" name="slug" value={slug} />
      <input type="hidden" name="email" value={email} />
      <input type="hidden" name="needsPassword" value={needsPassword ? "1" : "0"} />

      {state.error ? <Alert>{state.error}</Alert> : null}

      {needsPassword ? (
        <>
          <Field label="Choose a password" hint="At least 8 characters">
            <Input name="password" type="password" required minLength={8} autoComplete="new-password" />
          </Field>
          <Field label="Confirm password">
            <Input
              name="confirmPassword"
              type="password"
              required
              minLength={8}
              autoComplete="new-password"
            />
          </Field>
        </>
      ) : null}

      <SubmitButton className="w-full" pendingLabel="Joining…">
        {needsPassword ? "Create account and join" : "Join"}
      </SubmitButton>
    </form>
  );
}
