import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { firstValue } from "@/lib/listing";
import { getRole } from "@/lib/session";
import { categoryName, type Page as PageOf, type ReferenceValue } from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";
import { Pager } from "@/components/Pager";

/** The provinces are 38; a screenful of them is plenty at a time. */
const PER_PAGE = 20;

export const metadata = { title: "Reference list" };

/**
 * One of the tenant's own lists, whole.
 *
 * These decide what the dropdowns offer and what a record may store, and until
 * now they were only ever seen one option at a time inside a select. Reading
 * one — what it holds, in what order, what is switched off — meant querying the
 * database.
 */
export default async function ReferenceListPage({
  params,
  searchParams,
}: PageProps<"/organizations/[slug]/reference-data/[category]">) {
  const { slug, category } = await params;
  const resolved = await searchParams;
  const query = (firstValue(resolved.q) ?? "").trim();
  const page = Math.max(0, Number(firstValue(resolved.page)) || 0);
  const role = await getRole(slug);
  const backToOrganization = `/organizations/${slug}`;
  const wanted = decodeURIComponent(category).toUpperCase();

  if (!role) {
    return (
      <>
        <PageHeading title="Reference list" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  let values: PageOf<ReferenceValue> | null = null;
  let error: string | null = null;

  try {
    // The category endpoint, which answers 404 when the tenant keeps no such
    // list — the distinction an empty page cannot carry on its own.
    values = await api<PageOf<ReferenceValue>>(
      `/reference-data/${encodeURIComponent(wanted)}?page=${page}&size=${PER_PAGE}` +
        (query ? `&q=${encodeURIComponent(query)}` : ""),
      { tenant: slug },
    );
  } catch (e) {
    if (e instanceof ApiError) {
      error = e.status === 404 ? `This tenant keeps no list called ${wanted}.` : e.message;
    } else {
      error = "Cannot reach the API.";
    }
  }

  if (error || !values) {
    return (
      <>
        <Link
          href={backToOrganization}
          className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
        >
          ← Back to the organization
        </Link>
        <PageHeading title="Reference list" />
        <Alert>{error ?? "Not found."}</Alert>
      </>
    );
  }

  const live = values.content.filter((value) => value.active).length;

  return (
    <>
      <Link
        href={backToOrganization}
        className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
      >
        ← Back to the organization
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <PageHeading
          title={categoryName(wanted)}
          description="What the dropdowns offer, and what a record in this tenant may store."
        />
        {/* Of what is on this page, not of the list: counting the whole list
            would mean fetching the whole list, which is what paging is for. */}
        <Badge>
          {live} of {values.content.length} in use here
        </Badge>
      </div>

      <Card className="p-6">
        <form action={`/organizations/${slug}/reference-data/${wanted}`} className="mb-4 flex gap-2">
          <input
            type="search"
            name="q"
            defaultValue={query}
            placeholder="Search label or code"
            aria-label="Search this list"
            className="min-w-0 flex-1 rounded-lg border border-line bg-surface px-3 py-1.5 text-sm text-ink outline-none transition placeholder:text-ink-muted/70 focus:border-brand"
          />
          <button
            type="submit"
            className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-surface-muted"
          >
            Apply
          </button>
        </form>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs text-ink-muted">
                <th className="pb-2 font-medium">Label</th>
                <th className="pb-2 font-medium">Code</th>
                <th className="pb-2 text-right font-medium">Order</th>
                <th className="pb-2 text-right font-medium">In use</th>
                <th className="pb-2 text-right font-medium">Origin</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {values.content.map((value) => (
                <tr key={value.id} className={value.active ? "" : "text-ink-muted"}>
                  {/* The label is read, the code is stored. Both are shown
                      because the code is what a record actually holds and what
                      an API caller has to send. */}
                  <td className="py-3">{value.label}</td>
                  <td className="py-3 font-mono text-xs text-ink-muted">{value.code}</td>
                  <td className="py-3 text-right tabular-nums">{value.sortOrder}</td>
                  <td className="py-3 text-right">{value.active ? "Yes" : "No"}</td>
                  <td className="py-3 text-right text-xs text-ink-muted">
                    {value.systemDefined ? "System" : "Added here"}
                  </td>
                </tr>
              ))}
              {values.totalElements === 0 ? (
                <tr>
                  <td className="py-3 text-ink-muted" colSpan={5}>
                    {query
                      ? `Nothing in this list matches "${query}".`
                      : "This list is empty, so its dropdown offers nothing."}
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>

        <Pager
          href={(next) =>
            `/organizations/${slug}/reference-data/${wanted}?page=${next}` +
            (query ? `&q=${encodeURIComponent(query)}` : "")
          }
          page={values.page}
          size={values.size}
          totalElements={values.totalElements}
          totalPages={values.totalPages}
        />

        <p className="mt-4 text-xs text-ink-muted">
          {/* Said here because there is no edit button and somebody will look
              for one. */}
          These are read-only through the API. A tenant gets its own copy from a
          migration, and changing what the lists hold means changing that — an applied
          migration has to be corrected by a new version, never edited, or every tenant
          database fails its checksum.
        </p>
      </Card>
    </>
  );
}
