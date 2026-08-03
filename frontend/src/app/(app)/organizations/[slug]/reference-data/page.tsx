import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { firstValue } from "@/lib/listing";
import { getRole } from "@/lib/session";
import { categoryName, type Page as PageOf, type ReferenceValue } from "@/lib/types";
import { Alert, Card, PageHeading } from "@/components/ui";
import { Pager } from "@/components/Pager";

const PER_PAGE = 20;

export const metadata = { title: "Reference data" };

/**
 * Every value this tenant keeps, across the lists.
 *
 * One table is what this always was — a category is a column on it — so this is
 * the shape the data actually has, and the per-category screen is this one with
 * the category fixed. Which is why the category can be a filter here at all.
 */
export default async function ReferenceDataPage({
  params,
  searchParams,
}: PageProps<"/organizations/[slug]/reference-data">) {
  const { slug } = await params;
  const resolved = await searchParams;
  const query = (firstValue(resolved.q) ?? "").trim();
  const page = Math.max(0, Number(firstValue(resolved.page)) || 0);
  const chosen = (
    Array.isArray(resolved.category)
      ? resolved.category
      : resolved.category
        ? [resolved.category]
        : []
  ).filter((value): value is string => typeof value === "string" && value.length > 0);
  // Absent means both, which is not the same as either — so it is read as a
  // value rather than as a checkbox that is off.
  const active = firstValue(resolved.active);

  const role = await getRole(slug);
  if (!role) {
    return (
      <>
        <PageHeading title="Reference data" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  let values: PageOf<ReferenceValue> | null = null;
  let categories: string[] = [];
  let error: string | null = null;

  try {
    [values, categories] = await Promise.all([
      api<PageOf<ReferenceValue>>(
        `/reference-values?page=${page}&size=${PER_PAGE}` +
          (query ? `&q=${encodeURIComponent(query)}` : "") +
          (active === "true" || active === "false" ? `&active=${active}` : "") +
          chosen.map((value) => `&category=${encodeURIComponent(value)}`).join(""),
        { tenant: slug },
      ),
      api<string[]>("/reference-categories", { tenant: slug }),
    ]);
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  if (error || !values) {
    return (
      <>
        <Link
          href={`/organizations/${slug}`}
          className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
        >
          ← Back to the organization
        </Link>
        <PageHeading title="Reference data" />
        <Alert>{error ?? "Not found."}</Alert>
      </>
    );
  }

  const base = `/organizations/${slug}/reference-data`;
  const carried =
    (query ? `&q=${encodeURIComponent(query)}` : "") +
    (active === "true" || active === "false" ? `&active=${active}` : "") +
    chosen.map((value) => `&category=${encodeURIComponent(value)}`).join("");

  return (
    <>
      <Link
        href={`/organizations/${slug}`}
        className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
      >
        ← Back to the organization
      </Link>

      <PageHeading
        title="Reference data"
        description="Every list this tenant keeps, in one place. A record stores the code; the label is only ever shown."
      />

      <Card className="p-6">
        <form action={base} className="space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <input
              type="search"
              name="q"
              defaultValue={query}
              placeholder="Search label, code or list"
              aria-label="Search reference data"
              className="min-w-0 flex-1 rounded-lg border border-line bg-surface px-3 py-1.5 text-sm text-ink outline-none transition placeholder:text-ink-muted/70 focus:border-brand"
            />
            <button
              type="submit"
              className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-surface-muted"
            >
              Apply
            </button>
            {query || chosen.length > 0 || active ? (
              <Link href={base} className="px-2 py-1.5 text-sm text-ink-muted hover:text-ink">
                Clear
              </Link>
            ) : null}
          </div>

          <fieldset className="flex flex-wrap gap-x-4 gap-y-2 rounded-lg border border-line p-3">
            <legend className="px-1 text-xs text-ink-muted">List</legend>
            {categories.map((value) => (
              <label key={value} className="flex items-center gap-2 text-sm text-ink-muted">
                <input
                  type="checkbox"
                  name="category"
                  value={value}
                  defaultChecked={chosen.includes(value)}
                  className="size-4 rounded border-line"
                />
                {categoryName(value)}
              </label>
            ))}
          </fieldset>

          {/* Radios rather than checkboxes: a value is in use or it is not, and
              "both" is the absence of an answer rather than picking two. */}
          <fieldset className="flex flex-wrap items-center gap-4 rounded-lg border border-line p-3">
            <legend className="px-1 text-xs text-ink-muted">In use</legend>
            {(
              [
                ["", "Either"],
                ["true", "In use"],
                ["false", "Switched off"],
              ] as const
            ).map(([value, label]) => (
              <label key={label} className="flex items-center gap-2 text-sm text-ink-muted">
                <input
                  type="radio"
                  name="active"
                  value={value}
                  defaultChecked={(active ?? "") === value}
                  className="size-4 border-line"
                />
                {label}
              </label>
            ))}
          </fieldset>
        </form>

        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs text-ink-muted">
                <th className="pb-2 font-medium">List</th>
                <th className="pb-2 font-medium">Label</th>
                <th className="pb-2 font-medium">Code</th>
                <th className="pb-2 text-right font-medium">Order</th>
                <th className="pb-2 text-right font-medium">In use</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {values.content.map((value) => (
                <tr key={value.id} className={value.active ? "" : "text-ink-muted"}>
                  <td className="py-3">
                    <Link href={`${base}/${value.category}`} className="hover:underline">
                      {categoryName(value.category)}
                    </Link>
                  </td>
                  <td className="py-3">{value.label}</td>
                  <td className="py-3 font-mono text-xs text-ink-muted">{value.code}</td>
                  <td className="py-3 text-right tabular-nums">{value.sortOrder}</td>
                  <td className="py-3 text-right">{value.active ? "Yes" : "No"}</td>
                </tr>
              ))}
              {values.totalElements === 0 ? (
                <tr>
                  <td className="py-3 text-ink-muted" colSpan={5}>
                    Nothing here matches that.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>

        <Pager
          href={(next) => `${base}?page=${next}${carried}`}
          page={values.page}
          size={values.size}
          totalElements={values.totalElements}
          totalPages={values.totalPages}
        />
      </Card>
    </>
  );
}
