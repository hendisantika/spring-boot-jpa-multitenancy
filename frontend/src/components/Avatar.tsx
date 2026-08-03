/**
 * A stored photo, or the first letter of whatever names the thing when there is
 * none — a blank circle says nothing, and "no photo" is the common case.
 *
 * A plain `img` rather than `next/image`: the source is a signed URL that
 * changes on every render and points at whatever host the bucket is configured
 * as, so there is nothing to optimise or cache against.
 */
export function Avatar({
  photoUrl,
  email,
  /** Round for a face, square-ish for a place. */
  rounded = "full",
}: {
  photoUrl: string | null;
  email: string | null;
  rounded?: "full" | "lg";
}) {
  const initial = (email ?? "?").trim().charAt(0).toUpperCase() || "?";
  const shape = rounded === "lg" ? "rounded-lg" : "rounded-full";

  if (photoUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={photoUrl}
        alt=""
        className={`size-8 shrink-0 ${shape} border border-line object-cover`}
      />
    );
  }

  return (
    <span
      aria-hidden="true"
      className={`grid size-8 shrink-0 place-items-center ${shape} border border-line bg-surface-muted text-xs font-medium text-ink-muted`}
    >
      {initial}
    </span>
  );
}
