"use client";

import Link from "next/link";
import { useActionState } from "react";

import { savePerson } from "@/app/actions/tenant-data";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input, Select } from "@/components/ui";
import type { FormState, ReferenceLists, TenantPerson } from "@/lib/types";

export function PersonForm({
  slug,
  editing,
  backTo,
  lists,
}: {
  slug: string;
  editing: TenantPerson | null;
  /** Where saving and cancelling land, so a search and a page survive both. */
  backTo: string;
  /** The tenant's own reference lists, which is what the dropdowns hold. */
  lists: ReferenceLists;
}) {
  const [state, action] = useActionState<FormState, FormData>(savePerson, {});
  // What was submitted wins over what was loaded, so a rejected form keeps edits.
  const value = (field: keyof TenantPerson) =>
    state.values?.[field] ?? (editing?.[field] as string | null) ?? "";

  /**
   * A blank option first, because these are optional and "not recorded" is a
   * real answer.
   *
   * Only values still on offer are listed — but a record written before one was
   * retired still holds that code, so it is kept as its own option, by its real
   * label, and marked. Dropping it would silently rewrite the record the moment
   * anybody opened the form.
   */
  const options = (category: string, field: keyof TenantPerson) => {
    const values = lists[category] ?? [];
    const current = value(field);
    const offered = values.filter((option) => option.active);
    const retired = current && !offered.some((option) => option.code === current)
      ? values.find((option) => option.code === current)
      : null;
    return (
      <>
        <option value="">—</option>
        {offered.map((option) => (
          <option key={option.code} value={option.code}>
            {option.label}
          </option>
        ))}
        {current && !offered.some((option) => option.code === current) ? (
          <option value={current}>{(retired?.label ?? current) + " (no longer offered)"}</option>
        ) : null}
      </>
    );
  };

  return (
    // The key resets the uncontrolled inputs when switching between rows.
    <form action={action} className="space-y-4" key={editing?.id ?? "new"}>
      <input type="hidden" name="slug" value={slug} />
      <input type="hidden" name="backTo" value={backTo} />
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

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Date of birth" hint="Optional">
          <Input name="birthDate" type="date" defaultValue={value("birthDate")?.slice(0, 10)} />
        </Field>
        <Field label="Gender" hint="Optional">
          <Select name="gender" defaultValue={value("gender")}>
            {options("GENDER", "gender")}
          </Select>
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Marital status" hint="Optional">
          <Select name="maritalStatus" defaultValue={value("maritalStatus")}>
            {options("MARITAL_STATUS", "maritalStatus")}
          </Select>
        </Field>
        <Field label="Blood type" hint="Optional">
          <Select name="bloodType" defaultValue={value("bloodType")}>
            {options("BLOOD_TYPE", "bloodType")}
          </Select>
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Identity document" hint="Optional">
          <Select name="identityDocumentType" defaultValue={value("identityDocumentType")}>
            {options("IDENTITY_DOCUMENT", "identityDocumentType")}
          </Select>
        </Field>
        <Field label="Document number" hint="Optional">
          <Input name="identityNumber" maxLength={255} defaultValue={value("identityNumber")} />
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
        <SubmitButton pendingLabel="Saving…">{editing ? "Save changes" : "Add person"}</SubmitButton>
      </div>
    </form>
  );
}
