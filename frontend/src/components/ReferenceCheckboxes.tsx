import type { ReferenceLists } from "@/lib/types";

/**
 * A filter that takes several values at once, as checkboxes sharing one name —
 * which is how HTML has always submitted a repeated parameter, so it needs no
 * JavaScript.
 *
 * Folded into a `<details>` because a list like the provinces is 38 long and
 * would otherwise own the page. The summary says how many are chosen, so the
 * state is legible while it is shut.
 *
 * A `<select multiple>` would be shorter to write, but choosing several from
 * one means knowing to hold a modifier key, and clicking without it silently
 * throws the rest away.
 *
 * As everywhere else: only values still on offer are listed, except one already
 * chosen, which is kept by its real label and marked. Dropping it would quietly
 * widen somebody's saved filter the moment they opened it.
 */
export function ReferenceCheckboxes({
  label,
  name,
  category,
  lists,
  current,
}: {
  label: string;
  name: string;
  category: string;
  lists: ReferenceLists;
  current: string[];
}) {
  const values = lists[category] ?? [];
  const offered = values.filter((value) => value.active);
  const retired = current
    .filter((code) => !offered.some((value) => value.code === code))
    .map((code) => ({
      code,
      label: `${values.find((value) => value.code === code)?.label ?? code} (no longer offered)`,
    }));
  const options = [...offered.map(({ code, label: text }) => ({ code, label: text })), ...retired];

  return (
    <details className="group rounded-lg border border-line bg-surface" open={current.length > 0}>
      {/*
        The marker is drawn rather than left to the browser, because a summary
        with `list-none` and no replacement reads as a label nobody would think
        to click. It turns with the disclosure so the state is visible shut.
      */}
      <summary className="flex cursor-pointer list-none items-center gap-2 px-3 py-2 text-sm text-ink">
        <svg
          aria-hidden="true"
          viewBox="0 0 20 20"
          className="size-4 shrink-0 text-ink-muted transition-transform group-open:rotate-90"
          fill="currentColor"
        >
          <path d="M7 5l6 5-6 5V5z" />
        </svg>
        <span className="font-medium">{label}</span>
        <span className="text-ink-muted">
          {current.length === 0 ? "Any" : `${current.length} chosen`}
        </span>
      </summary>
      <fieldset className="max-h-48 overflow-y-auto border-t border-line px-3 py-2">
        <legend className="sr-only">{label}</legend>
        <div className="grid gap-1 sm:grid-cols-2">
          {options.map((option) => (
            <label key={option.code} className="flex items-center gap-2 text-sm text-ink">
              <input
                type="checkbox"
                name={name}
                value={option.code}
                defaultChecked={current.includes(option.code)}
                className="size-4 rounded border-line text-brand focus:ring-brand/30"
              />
              <span className="truncate">{option.label}</span>
            </label>
          ))}
        </div>
      </fieldset>
    </details>
  );
}
