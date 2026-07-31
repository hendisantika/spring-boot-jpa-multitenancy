"use client";

import { useActionState, useState } from "react";

import { signUp } from "@/app/actions/auth";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState } from "@/lib/types";

/** Matches the 5 MB cap and the types the backend accepts. */
const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
const ACCEPTED = "image/jpeg,image/png,image/webp";

export function SignupForm() {
  const [state, action] = useActionState<FormState, FormData>(signUp, {});
  const [preview, setPreview] = useState<string | null>(null);
  const [photoError, setPhotoError] = useState<string | null>(null);

  function onPhotoChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    setPhotoError(null);
    setPreview(null);
    if (!file) return;

    // Checked here as well as on the server, so the mistake is caught before a
    // 5 MB upload is attempted.
    if (file.size > MAX_PHOTO_BYTES) {
      setPhotoError("That photo is larger than 5 MB.");
      event.target.value = "";
      return;
    }
    setPreview(URL.createObjectURL(file));
  }

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

      <Field label="Photo" hint="Optional. JPEG, PNG or WebP, up to 5 MB.">
        <div className="flex items-center gap-3">
          {preview ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={preview}
              alt=""
              className="size-12 shrink-0 rounded-full border border-line object-cover"
            />
          ) : (
            <span className="grid size-12 shrink-0 place-items-center rounded-full border border-dashed border-line text-xs text-ink-muted">
              —
            </span>
          )}
          <Input name="photo" type="file" accept={ACCEPTED} onChange={onPhotoChange} className="py-1.5" />
        </div>
      </Field>
      {photoError ? <Alert>{photoError}</Alert> : null}

      <SubmitButton className="w-full" pendingLabel="Creating…">
        Create account
      </SubmitButton>
    </form>
  );
}
