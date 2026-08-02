"use client";

import { useActionState } from "react";

import { signUp } from "@/app/actions/auth";
import { PhotoField } from "@/components/PhotoField";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function SignupForm() {
  const [state, action] = useActionState<FormState, FormData>(signUp, {});

  return (
    <form action={action} className="space-y-4">
      {state.error ? <Alert>{state.error}</Alert> : null}

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

      <Field label="Phone number">
        <Input
          name="phoneNumber"
          type="tel"
          required
          autoComplete="tel"
          placeholder="+62 812 3456 7890"
          pattern="^\+?[0-9 ()-]{6,30}$"
          defaultValue={state.values?.phoneNumber ?? ""}
        />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Password" hint="At least 8 characters">
          <Input name="password" type="password" required minLength={8} autoComplete="new-password" />
        </Field>
        <Field label="Confirm password">
          <Input name="confirmPassword" type="password" required minLength={8} autoComplete="new-password" />
        </Field>
      </div>

      <PhotoField label="Photo" hint="Optional. JPEG, PNG or WebP, up to 5 MB." />

      <SubmitButton className="w-full" pendingLabel="Creating…">
        Create account
      </SubmitButton>
    </form>
  );
}
