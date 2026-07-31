"use client";

import Link from "next/link";
import { useActionState } from "react";

import { saveUnit } from "@/app/actions/tenant-data";
import { ReferenceSelect } from "@/components/ReferenceSelect";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState, ReferenceLists, TenantUnit } from "@/lib/types";

export function UnitForm({
  slug,
  editing,
  backTo,
  lists,
}: {
  slug: string;
  editing: TenantUnit | null;
  /** Where saving and cancelling land, so a search and a page survive both. */
  backTo: string;
  /** The tenant's own reference lists, which is what the dropdowns hold. */
  lists: ReferenceLists;
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

      <div className="grid gap-4 sm:grid-cols-2">
        <ReferenceSelect
          label="Kind of unit"
          name="unitType"
          category="UNIT_TYPE"
          lists={lists}
          current={value("unitType")}
        />
        <ReferenceSelect
          label="Operating status"
          name="operatingStatus"
          category="OPERATING_STATUS"
          lists={lists}
          current={value("operatingStatus")}
        />
      </div>

      <Field label="Address" hint="Optional">
        <Input name="address" maxLength={255} defaultValue={value("address")} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <ReferenceSelect
          label="Province"
          name="province"
          category="PROVINCE"
          lists={lists}
          current={value("province")}
        />
        <Field label="Email" hint="Optional">
          <Input name="email" type="email" maxLength={255} defaultValue={value("email")} />
        </Field>
      </div>

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
