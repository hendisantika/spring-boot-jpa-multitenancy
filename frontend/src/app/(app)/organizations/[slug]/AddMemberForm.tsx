"use client";

import { useActionState } from "react";

import { addMember } from "@/app/actions/organizations";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input, Select } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function AddMemberForm({ slug }: { slug: string }) {
  const [state, action] = useActionState<FormState, FormData>(addMember, {});

  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="slug" value={slug} />

      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.ok ? <Alert tone="info">Added. They can sign in now.</Alert> : null}

      <Field label="Email">
        <Input name="email" type="email" required placeholder="nurse@example.com" />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Phone number" hint="Optional">
          <Input name="phoneNumber" type="tel" pattern="^\+?[0-9 ()-]{6,30}$" />
        </Field>
        <Field label="Role">
          <Select name="role" defaultValue="MEMBER">
            <option value="MEMBER">Member</option>
            <option value="OWNER">Owner</option>
          </Select>
        </Field>
      </div>

      <Field label="Initial password" hint="At least 8 characters. Share it with them directly.">
        <Input name="password" type="password" required minLength={8} autoComplete="new-password" />
      </Field>

      <SubmitButton pendingLabel="Adding…">Add person</SubmitButton>
    </form>
  );
}
