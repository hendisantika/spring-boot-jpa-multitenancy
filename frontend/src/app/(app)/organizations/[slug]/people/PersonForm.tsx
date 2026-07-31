"use client";

import Link from "next/link";
import { useActionState } from "react";

import { savePerson } from "@/app/actions/tenant-data";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState, TenantPerson } from "@/lib/types";

export function PersonForm({ slug, editing }: { slug: string; editing: TenantPerson | null }) {
  const [state, action] = useActionState<FormState, FormData>(savePerson, {});
  // What was submitted wins over what was loaded, so a rejected form keeps edits.
  const value = (field: keyof TenantPerson) =>
    state.values?.[field] ?? (editing?.[field] as string | null) ?? "";

  return (
    // The key resets the uncontrolled inputs when switching between rows.
    <form action={action} className="space-y-4" key={editing?.id ?? "new"}>
      <input type="hidden" name="slug" value={slug} />
      {editing ? <input type="hidden" name="id" value={editing.id} /> : null}

      {state.error ? <Alert>{state.error}</Alert> : null}

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="First name">
          <Input name="firstName" required maxLength={100} defaultValue={value("firstName")} />
        </Field>
        <Field label="Last name">
          <Input name="lastName" required maxLength={100} defaultValue={value("lastName")} />
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Email" hint="Optional">
          <Input name="email" type="email" defaultValue={value("email")} />
        </Field>
        <Field label="Mobile" hint="Optional">
          <Input name="mobile" type="tel" defaultValue={value("mobile")} />
        </Field>
      </div>

      <Field label="Date of birth" hint="Optional">
        <Input name="birthDate" type="date" defaultValue={value("birthDate")?.slice(0, 10)} />
      </Field>

      <div className="flex items-center justify-end gap-3 border-t border-line pt-4">
        {editing ? (
          <Link
            href={`/organizations/${slug}/people`}
            className="rounded-lg border border-line px-4 py-2 text-sm text-ink transition hover:bg-surface-muted"
          >
            Cancel
          </Link>
        ) : null}
        <SubmitButton pendingLabel="Saving…">{editing ? "Save changes" : "Add person"}</SubmitButton>
      </div>
    </form>
  );
}
