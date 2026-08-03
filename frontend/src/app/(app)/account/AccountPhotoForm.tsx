"use client";

import { useActionState } from "react";

import { saveAccountPhoto } from "@/app/actions/account";
import { PhotoField } from "@/components/PhotoField";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function AccountPhotoForm({ photoUrl }: { photoUrl: string | null }) {
  const [state, action] = useActionState<FormState, FormData>(saveAccountPhoto, {});

  return (
    // Re-keyed on the stored photo so that after saving, the field starts again
    // from what is now stored rather than holding a preview of the old file.
    <form action={action} className="space-y-4" key={photoUrl ?? "none"}>
      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.ok ? <Alert tone="info">Saved.</Alert> : null}

      <PhotoField
        label="Photo"
        hint="JPEG, PNG or WebP, up to 5 MB."
        currentUrl={photoUrl}
      />

      <div className="flex items-center justify-end border-t border-line pt-4">
        <SubmitButton pendingLabel="Saving…">Save photo</SubmitButton>
      </div>
    </form>
  );
}
