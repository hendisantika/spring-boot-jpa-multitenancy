import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import {
  referenceLabel,
  type Page as PageOf,
  type ReferenceLists,
  type TenantPerson,
} from "@/lib/types";
import { Alert, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Person" };

/**
 * How many people are looked at to work out who sits either side. Past this the
 * steps are not offered, rather than being offered wrongly — a tenant's list of
 * people only grows, and this is the same window the units screen uses.
 */
const NEIGHBOURS_WINDOW = 200;

/** What a person is called in a link, which is not always a name. */
function nameOf(person: TenantPerson): string {
  return (
    [person.firstName, person.lastName].filter(Boolean).join(" ") || person.email || "Unnamed"
  );
}

/**
 * One person, whole. The list has room for a line each and the form is for
 * changing things; neither is a good place to simply read a record, and the
 * photo in particular was a 32px thumbnail of something worth looking at.
 */
export default async function PersonPage({ params }: PageProps<"/organizations/[slug]/people/[id]">) {
  const { slug, id } = await params;
  const role = await getRole(slug);
  const backToList = `/organizations/${slug}/people`;

  if (!role) {
    return (
      <>
        <PageHeading title="Person" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  let person: TenantPerson | null = null;
  let lists: ReferenceLists = {};
  let error: string | null = null;

  try {
    // The lists come too, because every coded field is stored as a code and
    // shown as a label, and one round trip is cheaper than five lookups.
    [person, lists] = await Promise.all([
      api<TenantPerson>(`/person/${id}`, { tenant: slug }),
      api<ReferenceLists>("/reference-data", { tenant: slug }),
    ]);
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  if (error || !person) {
    return (
      <>
        <Link href={backToList} className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
          ← Back to people
        </Link>
        <PageHeading title="Person" />
        <Alert>{error ?? "Not found."}</Alert>
      </>
    );
  }

  const name = [person.firstName, person.lastName].filter(Boolean).join(" ") || "—";

  // The neighbours, in the order the list shows them.
  const siblings = await api<PageOf<TenantPerson>>(`/person?size=${NEIGHBOURS_WINDOW}`, {
    tenant: slug,
  })
    .then((page) => page.content)
    .catch(() => []);
  const here = siblings.findIndex((candidate) => candidate.id === person.id);
  const previous = here > 0 ? siblings[here - 1] : null;
  const next = here >= 0 && here + 1 < siblings.length ? siblings[here + 1] : null;
  const document = [
    referenceLabel(lists.IDENTITY_DOCUMENT, person.identityDocumentType),
    person.identityNumber,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <>
      <Link href={backToList} className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
        ← Back to people
      </Link>

      <Card className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            {/*
              A plain img, like everywhere else: the source is signed, changes
              every render and points at whatever host the bucket is, so there
              is nothing to optimise against. Square rather than a circle — this
              is the size where a circle starts cropping faces.
            */}
            {person.photoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={person.photoUrl}
                alt=""
                className="size-28 shrink-0 rounded-xl border border-line object-cover"
              />
            ) : (
              <span
                aria-hidden="true"
                className="grid size-28 shrink-0 place-items-center rounded-xl border border-line bg-surface-muted text-3xl font-medium text-ink-muted"
              >
                {name.trim().charAt(0).toUpperCase() || "?"}
              </span>
            )}
            <div className="min-w-0">
              <h1 className="text-2xl font-semibold tracking-tight text-ink">{name}</h1>
              <p className="mt-1 text-sm text-ink-muted">{person.email || "No email"}</p>
              <p className="text-sm text-ink-muted">{person.mobile || "No mobile"}</p>
            </div>
          </div>

          {/* Editing stays on the list, where the form already lives. */}
          <Link
            href={`${backToList}?edit=${person.id}`}
            className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-surface-muted"
          >
            Edit
          </Link>
        </div>

        <dl className="mt-6 divide-y divide-line border-t border-line text-sm">
          {/* The one row that leads somewhere: from this person to everybody
              else at their unit, which is the question a unit answers. The
              filter takes the id, so the link carries it rather than the name
              that is read. */}
          <Row
            label="Unit"
            value={person.unitName}
            href={person.unitId ? `${backToList}?unit=${person.unitId}` : null}
          />
          <Row label="Date of birth" value={formatDate(person.birthDate)} />
          <Row label="Gender" value={referenceLabel(lists.GENDER, person.gender)} />
          <Row label="Marital status" value={referenceLabel(lists.MARITAL_STATUS, person.maritalStatus)} />
          <Row label="Blood type" value={referenceLabel(lists.BLOOD_TYPE, person.bloodType)} />
          <Row label="Identity document" value={document} />
        </dl>

        {previous || next ? (
          <nav
            aria-label="The other people"
            className="mt-4 flex items-center justify-between gap-3 border-t border-line pt-4 text-sm"
          >
            {previous ? (
              <Link
                href={`${backToList}/${previous.id}`}
                rel="prev"
                className="min-w-0 truncate text-ink-muted transition hover:text-ink"
              >
                ← {nameOf(previous)}
              </Link>
            ) : (
              <span />
            )}
            {next ? (
              <Link
                href={`${backToList}/${next.id}`}
                rel="next"
                className="min-w-0 truncate text-right text-ink-muted transition hover:text-ink"
              >
                {nameOf(next)} →
              </Link>
            ) : (
              <span />
            )}
          </nav>
        ) : null}
      </Card>
    </>
  );
}

function Row({
  label,
  value,
  href,
}: {
  label: string;
  value?: string | null;
  /** Where the value leads, when it leads anywhere. */
  href?: string | null;
}) {
  return (
    <div className="flex justify-between gap-4 py-3">
      <dt className="shrink-0 text-ink-muted">{label}</dt>
      <dd className="truncate text-right text-ink">
        {value && href ? (
          <Link href={href} className="hover:underline" title={`Everyone at ${value}`}>
            {value}
          </Link>
        ) : (
          value || "—"
        )}
      </dd>
    </div>
  );
}

/**
 * The API sends a calendar date, so it is read as one: building a Date from it
 * and formatting in the server's zone is exactly how a birthday moves a day.
 */
function formatDate(value: string | null): string | null {
  if (!value) return null;
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return value;
  return new Date(Date.UTC(year, month - 1, day)).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  });
}
