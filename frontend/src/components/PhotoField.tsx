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

  function onChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    setError(null);
    setPreview(null);
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
  // photo that would actually be saved.
  const shown = preview ?? currentUrl;
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
      {error ? <Alert>{error}</Alert> : null}
    </>
  );
}
