/**
 * The signed-in account's photo, or the first letter of its email when there is
 * none — a blank circle says nothing, and "no photo" is the common case.
 *
 * A plain `img` rather than `next/image`: the source is a signed URL that
 * changes on every render and points at whatever host the bucket is configured
 * as, so there is nothing to optimise or cache against.
 */
export function Avatar({ photoUrl, email }: { photoUrl: string | null; email: string | null }) {
  const initial = (email ?? "?").trim().charAt(0).toUpperCase() || "?";

  if (photoUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={photoUrl}
        alt=""
        className="size-8 shrink-0 rounded-full border border-line object-cover"
      />
    );
  }

  return (
    <span
      aria-hidden="true"
      className="grid size-8 shrink-0 place-items-center rounded-full border border-line bg-surface-muted text-xs font-medium text-ink-muted"
    >
      {initial}
    </span>
  );
}
