"use client";

import Link from "next/link";
import { useActionState } from "react";

import { logIn } from "@/app/actions/auth";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function LoginForm() {
  const [state, action] = useActionState<FormState, FormData>(logIn, {});

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

      <Field label="Password">
        <Input name="password" type="password" required autoComplete="current-password" />
      </Field>

      <p className="text-right">
        <Link href="/forgot-password" className="text-sm text-ink-muted hover:text-brand">
          Forgot your password?
        </Link>
      </p>

      <SubmitButton className="w-full" pendingLabel="Signing in…">
        Sign in
      </SubmitButton>
    </form>
  );
}
