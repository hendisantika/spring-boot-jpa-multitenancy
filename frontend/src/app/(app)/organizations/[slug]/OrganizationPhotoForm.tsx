"use client";

import { useActionState } from "react";

import { saveOrganizationPhoto } from "@/app/actions/organizations";
import { PhotoField } from "@/components/PhotoField";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert } from "@/components/ui";
import type { FormState } from "@/lib/types";

export function OrganizationPhotoForm({
  slug,
  photoUrl,
}: {
  slug: string;
  photoUrl: string | null;
}) {
  const [state, action] = useActionState<FormState, FormData>(saveOrganizationPhoto, {});

  return (
    // Re-keyed on the stored photo so that after saving, the field starts again
    // from what is now stored rather than holding a preview of the old file.
    <form action={action} className="space-y-4" key={photoUrl ?? "none"}>
      <input type="hidden" name="slug" value={slug} />
      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.ok ? <Alert tone="info">Saved.</Alert> : null}

      <PhotoField label="Photo" hint="JPEG, PNG or WebP, up to 5 MB." currentUrl={photoUrl} />

      <div className="flex items-center justify-end border-t border-line pt-4">
        <SubmitButton pendingLabel="Saving…">Save photo</SubmitButton>
      </div>
    </form>
  );
}
