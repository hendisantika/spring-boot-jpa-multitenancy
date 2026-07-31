import Link from "next/link";
import { redirect } from "next/navigation";

import { UnitForm } from "./UnitForm";
import { deleteUnit } from "@/app/actions/tenant-data";
import { ApiError, api } from "@/lib/api";
import { PAGE_SIZE, firstValue, listingQuery, listingUrl } from "@/lib/listing";
import { getRole } from "@/lib/session";
import type { Page, ReferenceLists, TenantUnit } from "@/lib/types";
import { referenceLabel } from "@/lib/types";
import { Pager } from "@/components/Pager";
import { SearchBox } from "@/components/SearchBox";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Business units" };

const EMPTY: Page<TenantUnit> = { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 };

/** The reference fields worth seeing without opening the row, as labels. */
function describe(unit: TenantUnit, lists: ReferenceLists): string {
  return [
    referenceLabel(lists.UNIT_TYPE, unit.unitType),
    referenceLabel(lists.PROVINCE, unit.province),
    referenceLabel(lists.OPERATING_STATUS, unit.operatingStatus),
  ]
    .filter(Boolean)
    .join(" · ");
}

/**
 * The backend calls these organizations, which collides with the organization
 * that owns the tenant, so the UI does not.
 *
 * A member may read and search them but not change them, so the search box is
 * offered to everybody while the form is not.
 */
export default async function UnitsPage({ params, searchParams }: PageProps<"/organizations/[slug]/units">) {
  const { slug } = await params;
  const { edit, q, page } = await searchParams;
  const role = await getRole(slug);

  if (!role) {
    return (
      <>
        <PageHeading title="Business units" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  const base = `/organizations/${slug}/units`;
  const query = firstValue(q).trim();
  const requested = Math.max(0, Number(firstValue(page)) || 0);

  let units = EMPTY;
  let lists: ReferenceLists = {};
  let error: string | null = null;

  try {
    // Both at once: the form needs the lists whether or not it is editing, and
    // waiting for the units first would only make the page slower.
    [units, lists] = await Promise.all([
      api<Page<TenantUnit>>(`/organization?${listingQuery(query, requested)}`, { tenant: slug }),
      api<ReferenceLists>("/reference-data", { tenant: slug }),
    ]);
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  // Deleting the last row of the last page leaves you standing past the end.
  if (units.totalElements > 0 && units.content.length === 0 && requested >= units.totalPages) {
    redirect(listingUrl(base, query, units.totalPages - 1));
  }

  const here = (extra?: Record<string, string>) => listingUrl(base, query, units.page, extra);
  const editingId = Number(firstValue(edit));
  const editing = units.content.find((unit) => unit.id === editingId) ?? null;

  return (
    <>
      <Link href={`/organizations/${slug}`} className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
        ← Back
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <PageHeading
          title="Business units"
          description={`In the ${slug} database. Everyone can read them; only the owner can change them.`}
        />
        <Badge tone={role === "OWNER" ? "brand" : "muted"}>You are {role}</Badge>
      </div>

      {error ? <Alert>{error}</Alert> : null}

      <div className="grid gap-6 lg:grid-cols-[1.2fr_1fr]">
        <Card className="p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-semibold text-ink">Units</h2>
            <span className="text-sm text-ink-muted">{units.totalElements}</span>
          </div>

          <div className="mb-4">
            <SearchBox
              action={base}
              query={query}
              placeholder="Search name, address or email"
              label="Search business units"
            />
          </div>

          <ul className="divide-y divide-line">
            {units.content.map((unit) => (
              <li key={unit.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm text-ink">{unit.name || "—"}</p>
                  <p className="truncate text-xs text-ink-muted">
                    {[unit.address, unit.email].filter(Boolean).join(" · ") || "No details"}
                  </p>
                  {/* Labels, never codes: a code is storage, not something to read. */}
                  {describe(unit, lists) ? (
                    <p className="truncate text-xs text-ink-muted/70">{describe(unit, lists)}</p>
                  ) : null}
                </div>
                {role === "OWNER" ? (
                  <div className="flex shrink-0 items-center gap-1">
                    <Link
                      href={here({ edit: String(unit.id) })}
                      className="rounded-md px-2 py-1 text-xs text-ink-muted transition hover:bg-surface-muted hover:text-ink"
                    >
                      Edit
                    </Link>
                    <form action={deleteUnit}>
                      <input type="hidden" name="slug" value={slug} />
                      <input type="hidden" name="id" value={unit.id} />
                      <button
                        type="submit"
                        className="rounded-md px-2 py-1 text-xs text-ink-muted transition hover:bg-danger/10 hover:text-danger"
                      >
                        Delete
                      </button>
                    </form>
                  </div>
                ) : null}
              </li>
            ))}
            {units.content.length === 0 && !error ? (
              <li className="py-3 text-sm text-ink-muted">
                {query ? `Nothing matches “${query}”.` : "Nothing yet."}
              </li>
            ) : null}
          </ul>

          <Pager
            href={(target) => listingUrl(base, query, target)}
            page={units.page}
            size={units.size}
            totalElements={units.totalElements}
            totalPages={units.totalPages}
          />
        </Card>

        {/* Not offered to a member, because the API refuses the write anyway. */}
        {role === "OWNER" ? (
          <Card className="p-6">
            <h2 className="mb-4 font-semibold text-ink">{editing ? "Edit unit" : "Add a unit"}</h2>
            <UnitForm slug={slug} editing={editing} backTo={here()} lists={lists} />
          </Card>
        ) : (
          <Card className="p-6">
            <p className="text-sm text-ink-muted">
              Only the owner of this organization can add or change business units.
            </p>
          </Card>
        )}
      </div>
    </>
  );
}
