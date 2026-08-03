"use client";

import { useState } from "react";

import { Alert, Field, Input } from "@/components/ui";

/** Matches the backend's `application.storage.max-file-size`. */
export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

/** The types the backend accepts. */
export const ACCEPTED_PHOTO_TYPES = "image/jpeg,image/png,image/webp";

/**
 * Picking a photo, with the size checked before anything is uploaded.
 *
 * The check is a courtesy — the API refuses an oversized file regardless — but
 * without it the mistake costs a full upload before anybody hears about it, and
 * a file over the server action's body limit fails as an unhandled error rather
 * than as a message.
 *
 * Shared so all three forms that take a photo behave the same. They did not:
 * signup previewed and checked, while the two organization forms did neither.
 */
export function PhotoField({
  label = "Photo",
  hint,
  name = "photo",
  /** The stored photo, shown until a new one is picked. */
  currentUrl = null,
  round = true,
}: {
  label?: string;
  hint?: string;
  name?: string;
  currentUrl?: string | null;
  round?: boolean;
}) {
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [removing, setRemoving] = useState(false);

  function onChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    setError(null);
    setPreview(null);
    // Choosing a file and asking to remove one are contradictory, so picking a
    // file un-asks the removal rather than leaving the server to guess.
    setRemoving(false);
    if (!file) return;

    if (file.size > MAX_PHOTO_BYTES) {
      const megabytes = (file.size / 1024 / 1024).toFixed(1);
      setError(`That photo is ${megabytes} MB. The limit is 5 MB.`);
      // Cleared, so submitting cannot send what was just refused.
      event.target.value = "";
      return;
    }
    setPreview(URL.createObjectURL(file));
  }

  // What was just picked wins over what is stored, so the preview shows the
  // photo that would actually be saved — and nothing at all once removal is
  // asked for, since that is what saving would leave.
  const shown = preview ?? (removing ? null : currentUrl);
  const shape = round ? "rounded-full" : "rounded-lg";

  return (
    <>
      <Field label={label} hint={hint}>
        <div className="flex items-center gap-3">
          {shown ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={shown}
              alt=""
              className={`size-12 shrink-0 border border-line object-cover ${shape}`}
            />
          ) : (
            <span
              className={`grid size-12 shrink-0 place-items-center border border-dashed border-line text-xs text-ink-muted ${shape}`}
            >
              —
            </span>
          )}
          <Input
            name={name}
            type="file"
            accept={ACCEPTED_PHOTO_TYPES}
            onChange={onChange}
            className="py-1.5"
          />
        </div>
      </Field>
      {/*
        Only offered when there is something to remove. A checkbox that can
        never do anything is worse than no checkbox.
      */}
      {currentUrl ? (
        <label className="flex items-center gap-2 text-sm text-ink-muted">
          <input
            type="checkbox"
            name="removePhoto"
            value="true"
            checked={removing}
            onChange={(event) => {
              setRemoving(event.target.checked);
              if (event.target.checked) {
                // Ticking it clears whatever was picked, for the same reason.
                setPreview(null);
                const input = event.target.form?.elements.namedItem(name);
                if (input instanceof HTMLInputElement) input.value = "";
              }
            }}
            className="size-4 rounded border-line text-brand focus:ring-brand/30"
          />
          Remove the current photo
        </label>
      ) : null}

      {error ? <Alert>{error}</Alert> : null}
    </>
  );
}
