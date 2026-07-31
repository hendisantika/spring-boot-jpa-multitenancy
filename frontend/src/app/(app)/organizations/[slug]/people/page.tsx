import Link from "next/link";
import { redirect } from "next/navigation";

import { PersonForm } from "./PersonForm";
import { deletePerson } from "@/app/actions/tenant-data";
import { ApiError, api } from "@/lib/api";
import { PAGE_SIZE, firstValue, listingQuery, listingUrl } from "@/lib/listing";
import { getRole } from "@/lib/session";
import type { Page, ReferenceLists, TenantPerson } from "@/lib/types";
import { referenceLabel } from "@/lib/types";
import { Pager } from "@/components/Pager";
import { SearchBox } from "@/components/SearchBox";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "People" };

const EMPTY: Page<TenantPerson> = { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 };

/** The reference fields worth seeing without opening the row, as labels. */
function describe(person: TenantPerson, lists: ReferenceLists): string {
  return [
    referenceLabel(lists.GENDER, person.gender),
    referenceLabel(lists.BLOOD_TYPE, person.bloodType),
    referenceLabel(lists.IDENTITY_DOCUMENT, person.identityDocumentType),
  ]
    .filter(Boolean)
    .join(" · ");
}

/**
 * This is the tenant's own data, read through its own database rather than the
 * central one, so every call carries the tenant.
 */
export default async function PeoplePage({ params, searchParams }: PageProps<"/organizations/[slug]/people">) {
  const { slug } = await params;
  const { edit, q, page } = await searchParams;
  const role = await getRole(slug);

  if (!role) {
    return (
      <>
        <PageHeading title="People" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  const base = `/organizations/${slug}/people`;
  const query = firstValue(q).trim();
  const requested = Math.max(0, Number(firstValue(page)) || 0);

  let people = EMPTY;
  let lists: ReferenceLists = {};
  let error: string | null = null;

  try {
    // Both at once: the form needs the lists whether or not it is editing, and
    // waiting for the people first would only make the page slower.
    [people, lists] = await Promise.all([
      api<Page<TenantPerson>>(`/person?${listingQuery(query, requested)}`, { tenant: slug }),
      api<ReferenceLists>("/reference-data", { tenant: slug }),
    ]);
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  // Deleting the last row of the last page leaves you standing past the end.
  if (people.totalElements > 0 && people.content.length === 0 && requested >= people.totalPages) {
    redirect(listingUrl(base, query, people.totalPages - 1));
  }

  const here = (extra?: Record<string, string>) => listingUrl(base, query, people.page, extra);
  const editingId = Number(firstValue(edit));
  const editing = people.content.find((person) => person.id === editingId) ?? null;

  return (
    <>
      <Link href={`/organizations/${slug}`} className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
        ← Back
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <PageHeading
          title="People"
          description={`In the ${slug} database. Members can add and edit; only the owner can delete.`}
        />
        <Badge tone={role === "OWNER" ? "brand" : "muted"}>You are {role}</Badge>
      </div>

      {error ? <Alert>{error}</Alert> : null}

      <div className="grid gap-6 lg:grid-cols-[1.2fr_1fr]">
        <Card className="p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-semibold text-ink">Everyone</h2>
            <span className="text-sm text-ink-muted">{people.totalElements}</span>
          </div>

          <div className="mb-4">
            <SearchBox
              action={base}
              query={query}
              placeholder="Search name, email or mobile"
              label="Search people"
            />
          </div>

          <ul className="divide-y divide-line">
            {people.content.map((person) => (
              <li key={person.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm text-ink">
                    {[person.firstName, person.lastName].filter(Boolean).join(" ") || "—"}
                  </p>
                  <p className="truncate text-xs text-ink-muted">
                    {[person.email, person.mobile].filter(Boolean).join(" · ") || "No contact details"}
                  </p>
                  {/* Labels, never codes: a code is storage, not something to read. */}
                  {describe(person, lists) ? (
                    <p className="truncate text-xs text-ink-muted/70">{describe(person, lists)}</p>
                  ) : null}
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <Link
                    href={here({ edit: String(person.id) })}
                    className="rounded-md px-2 py-1 text-xs text-ink-muted transition hover:bg-surface-muted hover:text-ink"
                  >
                    Edit
                  </Link>
                  {/* Offered only to an owner, because the API refuses it for anyone else. */}
                  {role === "OWNER" ? (
                    <form action={deletePerson}>
                      <input type="hidden" name="slug" value={slug} />
                      <input type="hidden" name="id" value={person.id} />
                      <button
                        type="submit"
                        className="rounded-md px-2 py-1 text-xs text-ink-muted transition hover:bg-danger/10 hover:text-danger"
                      >
                        Delete
                      </button>
                    </form>
                  ) : null}
                </div>
              </li>
            ))}
            {people.content.length === 0 && !error ? (
              <li className="py-3 text-sm text-ink-muted">
                {query ? `Nobody matches “${query}”.` : "Nobody yet."}
              </li>
            ) : null}
          </ul>

          <Pager
            href={(target) => listingUrl(base, query, target)}
            page={people.page}
            size={people.size}
            totalElements={people.totalElements}
            totalPages={people.totalPages}
          />
        </Card>

        <Card className="p-6">
          <h2 className="mb-4 font-semibold text-ink">{editing ? "Edit person" : "Add someone"}</h2>
          <PersonForm slug={slug} editing={editing} backTo={here()} lists={lists} />
        </Card>
      </div>
    </>
  );
}
