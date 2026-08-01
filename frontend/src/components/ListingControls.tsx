import Link from "next/link";

import { ReferenceCheckboxes } from "@/components/ReferenceCheckboxes";
import { ReferenceSelect } from "@/components/ReferenceSelect";
import { Input } from "@/components/ui";
import type { Listing } from "@/lib/listing";
import { chosen, chosenAll, isNarrowed } from "@/lib/listing";
import type { ReferenceLists } from "@/lib/types";

/** One filter offered above a list. */
export type FilterField = {
  /** The query parameter, which is also the field on the record. */
  name: string;
  label: string;
  /** Which list in `lists` to draw from, such as `PROVINCE`. */
  category: string;
  /**
   * Whether several values may be chosen at once, which then mean either of
   * them. Worth it for a long list; for a handful of values, choosing two is
   * close enough to choosing none that it only adds a control.
   */
  multiple?: boolean;
};

/**
 * The search box and the filters, in one plain GET form, so narrowing a list is
 * a URL. That makes a result shareable, keeps the back button honest, and works
 * before any JavaScript has loaded.
 *
 * One form rather than one per control, because they combine: applying a filter
 * must not throw away what was typed in the box, and vice versa.
 *
 * The page number is deliberately not carried: page 4 of the old results says
 * nothing about the new ones.
 *
 * There is a button rather than a submit-on-change, so several filters can be
 * set before the page reloads once — and so none of it depends on JavaScript.
 */
export function ListingControls({
  listing,
  placeholder,
  label,
  lists,
  filters,
}: {
  listing: Listing;
  placeholder: string;
  label: string;
  lists: ReferenceLists;
  filters: FilterField[];
}) {
  const single = filters.filter((filter) => !filter.multiple);
  const many = filters.filter((filter) => filter.multiple);

  return (
    <form action={listing.base} method="get" className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <Input
          name="q"
          type="search"
          defaultValue={listing.query}
          placeholder={placeholder}
          aria-label={label}
          className="max-w-xs"
        />
        <button
          type="submit"
          className="rounded-lg border border-line px-3 py-2 text-sm text-ink transition hover:bg-surface-muted"
        >
          Apply
        </button>
        {isNarrowed(listing) ? (
          <Link
            href={listing.base}
            className="rounded-lg px-2 py-2 text-sm text-ink-muted transition hover:text-ink"
          >
            Clear
          </Link>
        ) : null}
      </div>

      {single.length ? (
        <div className="grid gap-3 sm:grid-cols-2">
          {single.map((filter) => (
            <ReferenceSelect
              key={filter.name}
              label={filter.label}
              name={filter.name}
              category={filter.category}
              lists={lists}
              current={chosen(listing, filter.name)}
              blank="Any"
              hint=""
            />
          ))}
        </div>
      ) : null}

      {many.map((filter) => (
        <ReferenceCheckboxes
          key={filter.name}
          label={filter.label}
          name={filter.name}
          category={filter.category}
          lists={lists}
          current={chosenAll(listing, filter.name)}
        />
      ))}
    </form>
  );
}
