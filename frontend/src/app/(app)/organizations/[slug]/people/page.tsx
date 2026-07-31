import Link from "next/link";
import { redirect } from "next/navigation";

import { Pager } from "./Pager";
import { PeopleSearch } from "./PeopleSearch";
import { PersonForm } from "./PersonForm";
import { deletePerson } from "@/app/actions/tenant-data";
import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import type { Page, TenantPerson } from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "People" };

const PAGE_SIZE = 10;

const EMPTY: Page<TenantPerson> = { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 };

function first(value: string | string[] | undefined): string {
  return typeof value === "string" ? value : "";
}

/**
 * This is the tenant's own data, read through its own database rather than the
 * central one, so every call carries the tenant.
 *
 * The search and the page live in the URL rather than in state, which is what
 * lets a link to page 3 of "santoso" still mean that when it is opened again.
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

  const query = first(q).trim();
  const requested = Math.max(0, Number(first(page)) || 0);

  const search = new URLSearchParams({ page: String(requested), size: String(PAGE_SIZE) });
  if (query) search.set("q", query);

  let people = EMPTY;
  let error: string | null = null;

  try {
    people = await api<Page<TenantPerson>>(`/person?${search}`, { tenant: slug });
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  const pageUrl = (target: number) => {
    const params = new URLSearchParams();
    if (query) params.set("q", query);
    if (target > 0) params.set("page", String(target));
    const suffix = params.toString();
    return `/organizations/${slug}/people${suffix ? `?${suffix}` : ""}`;
  };

  // Deleting the last row of the last page leaves you standing past the end.
  if (people.totalElements > 0 && people.content.length === 0 && requested >= people.totalPages) {
    redirect(pageUrl(people.totalPages - 1));
  }

  /** Every link back into this screen keeps whatever the search and page are. */
  const urlFor = (extra: Record<string, string> = {}) => {
    const target = new URLSearchParams();
    if (query) target.set("q", query);
    if (people.page > 0) target.set("page", String(people.page));
    for (const [key, value] of Object.entries(extra)) target.set(key, value);
    const suffix = target.toString();
    return `/organizations/${slug}/people${suffix ? `?${suffix}` : ""}`;
  };

  const editingId = Number(first(edit));
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
            <PeopleSearch slug={slug} query={query} />
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
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <Link
                    href={urlFor({ edit: String(person.id) })}
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
            href={pageUrl}
            page={people.page}
            size={people.size}
            totalElements={people.totalElements}
            totalPages={people.totalPages}
          />
        </Card>

        <Card className="p-6">
          <h2 className="mb-4 font-semibold text-ink">{editing ? "Edit person" : "Add someone"}</h2>
          <PersonForm slug={slug} editing={editing} backTo={urlFor()} />
        </Card>
      </div>
    </>
  );
}
