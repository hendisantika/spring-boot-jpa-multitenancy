import Link from "next/link";

import { Input } from "@/components/ui";

/**
 * A plain GET form, so searching is a URL. That makes a result shareable, keeps
 * the back button honest, and works before any JavaScript has loaded.
 *
 * Submitting drops the page number on purpose: page 4 of the old results says
 * nothing about the new ones.
 */
export function SearchBox({
  action,
  query,
  placeholder,
  label,
}: {
  /** Where the form submits, which is the screen it sits on. */
  action: string;
  query: string;
  placeholder: string;
  label: string;
}) {
  return (
    <form action={action} method="get" className="flex items-center gap-2">
      <Input
        name="q"
        type="search"
        defaultValue={query}
        placeholder={placeholder}
        aria-label={label}
        className="max-w-xs"
      />
      <button
        type="submit"
        className="rounded-lg border border-line px-3 py-2 text-sm text-ink transition hover:bg-surface-muted"
      >
        Search
      </button>
      {query ? (
        <Link href={action} className="rounded-lg px-2 py-2 text-sm text-ink-muted transition hover:text-ink">
          Clear
        </Link>
      ) : null}
    </form>
  );
}
