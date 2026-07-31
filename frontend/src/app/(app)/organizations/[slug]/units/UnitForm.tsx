"use client";

import Link from "next/link";
import { useActionState } from "react";

import { saveUnit } from "@/app/actions/tenant-data";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState, TenantUnit } from "@/lib/types";

export function UnitForm({
  slug,
  editing,
  backTo,
}: {
  slug: string;
  editing: TenantUnit | null;
  /** Where saving and cancelling land, so a search and a page survive both. */
  backTo: string;
}) {
  const [state, action] = useActionState<FormState, FormData>(saveUnit, {});
  const value = (field: keyof TenantUnit) =>
    state.values?.[field] ?? (editing?.[field] as string | null) ?? "";

  return (
    <form action={action} className="space-y-4" key={editing?.id ?? "new"}>
      <input type="hidden" name="slug" value={slug} />
      <input type="hidden" name="backTo" value={backTo} />
      {editing ? <input type="hidden" name="id" value={editing.id} /> : null}

      {state.error ? <Alert>{state.error}</Alert> : null}

      <Field label="Name">
        <Input name="name" required maxLength={255} defaultValue={value("name")} />
      </Field>

      <Field label="Address" hint="Optional">
        <Input name="address" maxLength={255} defaultValue={value("address")} />
      </Field>

      <Field label="Email" hint="Optional">
        <Input name="email" type="email" maxLength={255} defaultValue={value("email")} />
      </Field>

      <div className="flex items-center justify-end gap-3 border-t border-line pt-4">
        {editing ? (
          <Link
            href={backTo}
            className="rounded-lg border border-line px-4 py-2 text-sm text-ink transition hover:bg-surface-muted"
          >
            Cancel
          </Link>
        ) : null}
        <SubmitButton pendingLabel="Saving…">{editing ? "Save changes" : "Add unit"}</SubmitButton>
      </div>
    </form>
  );
}
