"use client";

import { useActionState, useRef } from "react";

import { saveAccountPassword } from "@/app/actions/account";
import { PasswordInput } from "@/components/PasswordInput";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function AccountPasswordForm() {
  const formRef = useRef<HTMLFormElement>(null);
  // Cleared here rather than by re-keying the form: there is nothing stored to
  // key on, and three passwords should not sit in the page after a save.
  const [state, action] = useActionState<FormState, FormData>(async (previous, data) => {
    const result = await saveAccountPassword(previous, data);
    if (result.ok) formRef.current?.reset();
    return result;
  }, {});

  return (
    <form action={action} ref={formRef} className="space-y-4">
      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.ok ? <Alert tone="info">Saved. Other sessions have been signed out.</Alert> : null}

      <Field label="Current password">
        <PasswordInput name="currentPassword" required autoComplete="current-password" />
      </Field>

      <Field label="New password" hint="At least 8 characters.">
        <PasswordInput name="newPassword" required minLength={8} autoComplete="new-password" />
      </Field>

      <Field label="Confirm new password">
        <PasswordInput name="confirmPassword" required minLength={8} autoComplete="new-password" />
      </Field>

      <div className="flex items-center justify-end border-t border-line pt-4">
        <SubmitButton pendingLabel="Saving…">Change password</SubmitButton>
      </div>
    </form>
  );
}
