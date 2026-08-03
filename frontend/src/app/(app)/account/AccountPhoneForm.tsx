"use client";

import { useActionState } from "react";

import { saveAccountPhone } from "@/app/actions/account";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function AccountPhoneForm({ phoneNumber }: { phoneNumber: string | null }) {
  const [state, action] = useActionState<FormState, FormData>(saveAccountPhone, {});

  return (
    <form action={action} className="space-y-4">
      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.ok ? <Alert tone="info">Saved.</Alert> : null}

      <Field label="Phone" hint="Digits, spaces, brackets and dashes; a leading + is allowed.">
        <Input
          name="phoneNumber"
          type="tel"
          required
          maxLength={30}
          autoComplete="tel"
          // What was submitted wins, so a refused number stays put to be fixed
          // rather than reverting to the stored one and losing the correction.
          defaultValue={state.values?.phoneNumber ?? phoneNumber ?? ""}
        />
      </Field>

      <div className="flex items-center justify-end border-t border-line pt-4">
        <SubmitButton pendingLabel="Saving…">Save phone</SubmitButton>
      </div>
    </form>
  );
}
