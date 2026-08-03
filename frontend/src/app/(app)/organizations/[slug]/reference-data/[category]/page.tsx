import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import { categoryName, type ReferenceLists, type ReferenceValue } from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

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
}: PageProps<"/organizations/[slug]/reference-data/[category]">) {
  const { slug, category } = await params;
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

  let lists: ReferenceLists | null = null;
  let error: string | null = null;

  try {
    // The whole map rather than /reference-data/{category}: that endpoint
    // answers an unknown category with an empty list, deliberately, because an
    // absent dropdown is not an error. A screen has to tell "this tenant keeps
    // no such list" from "the list is empty", and the map says which.
    lists = await api<ReferenceLists>("/reference-data", { tenant: slug });
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  const values: ReferenceValue[] | undefined = lists?.[wanted];

  if (error || !lists || !values) {
    return (
      <>
        <Link
          href={backToOrganization}
          className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
        >
          ← Back to the organization
        </Link>
        <PageHeading title="Reference list" />
        <Alert>{error ?? `This tenant keeps no list called ${wanted}.`}</Alert>
      </>
    );
  }

  const live = values.filter((value) => value.active).length;

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
        <Badge>
          {live} of {values.length} in use
        </Badge>
      </div>

      <Card className="p-6">
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
              {values.map((value) => (
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
              {values.length === 0 ? (
                <tr>
                  <td className="py-3 text-ink-muted" colSpan={5}>
                    This list is empty, so its dropdown offers nothing.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>

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
