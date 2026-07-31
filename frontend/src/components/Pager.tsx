import Link from "next/link";

/**
 * Links rather than buttons, so a page is a place you can bookmark or go back
 * to. The step either side is rendered as plain text when there is nowhere to
 * go, which keeps the row from jumping about as you move through it.
 */
export function Pager({
  href,
  page,
  size,
  totalElements,
  totalPages,
}: {
  href: (page: number) => string;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}) {
  if (totalElements === 0) return null;

  const first = page * size + 1;
  const last = Math.min(first + size - 1, totalElements);
  const step = "rounded-md border border-line px-2 py-1 text-xs transition";

  return (
    <div className="mt-4 flex items-center justify-between border-t border-line pt-4">
      <p className="text-xs text-ink-muted">
        {first}–{last} of {totalElements}
      </p>
      <div className="flex items-center gap-1">
        {page > 0 ? (
          <Link href={href(page - 1)} className={`${step} text-ink hover:bg-surface-muted`} rel="prev">
            Previous
          </Link>
        ) : (
          <span className={`${step} text-ink-muted/50`}>Previous</span>
        )}
        <span className="px-2 text-xs text-ink-muted">
          {page + 1} / {Math.max(totalPages, 1)}
        </span>
        {page + 1 < totalPages ? (
          <Link href={href(page + 1)} className={`${step} text-ink hover:bg-surface-muted`} rel="next">
            Next
          </Link>
        ) : (
          <span className={`${step} text-ink-muted/50`}>Next</span>
        )}
      </div>
    </div>
  );
}
