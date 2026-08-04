"use client";

import { useActionState } from "react";

import { resetPassword } from "@/app/actions/password";
import { PasswordInput } from "@/components/PasswordInput";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function ResetPasswordForm({ token }: { token: string }) {
  const [state, action] = useActionState<FormState, FormData>(resetPassword, {});

  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="token" value={token} />

      {state.error ? <Alert>{state.error}</Alert> : null}

      <Field label="New password" hint="At least 8 characters">
        <PasswordInput name="password" required minLength={8} autoComplete="new-password" />
      </Field>

      <Field label="Confirm password">
        <PasswordInput name="confirmPassword" required minLength={8} autoComplete="new-password" />
      </Field>

      <SubmitButton className="w-full" pendingLabel="Saving…">
        Save new password
      </SubmitButton>
    </form>
  );
}
