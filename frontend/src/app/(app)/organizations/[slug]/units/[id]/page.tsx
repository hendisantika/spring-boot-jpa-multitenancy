import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import {
  referenceLabel,
  type Page as PageOf,
  type ReferenceLists,
  type TenantPerson,
  type TenantUnit,
} from "@/lib/types";
import { Alert, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Business unit" };

/**
 * How many units are looked at to work out which ones sit either side. Past
 * this the steps are simply not offered, rather than being offered wrongly.
 */
const NEIGHBOURS_WINDOW = 200;

/**
 * One unit, whole — the same reasoning as a person: the list has a line each
 * and the form is for changing things, so neither is a place to read a record.
 */
export default async function UnitPage({ params }: PageProps<"/organizations/[slug]/units/[id]">) {
  const { slug, id } = await params;
  const role = await getRole(slug);
  const backToList = `/organizations/${slug}/units`;

  if (!role) {
    return (
      <>
        <PageHeading title="Business unit" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  let unit: TenantUnit | null = null;
  let lists: ReferenceLists = {};
  let error: string | null = null;

  try {
    // The lists come too, because every coded field is stored as a code and
    // shown as a label, and one round trip is cheaper than three lookups.
    [unit, lists] = await Promise.all([
      api<TenantUnit>(`/organization/${id}`, { tenant: slug }),
      api<ReferenceLists>("/reference-data", { tenant: slug }),
    ]);
  } catch (e) {
    // The API says "organization" for these, which is the name this UI avoids
    // precisely because it means the tenant elsewhere. A 404 is the one message
    // worth rewording; anything else is the API describing its own problem.
    if (e instanceof ApiError) {
      error = e.status === 404 ? `No business unit with id ${id}.` : e.message;
    } else {
      error = "Cannot reach the API.";
    }
  }

  if (error || !unit) {
    return (
      <>
        <Link href={backToList} className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
          ← Back to units
        </Link>
        <PageHeading title="Business unit" />
        <Alert>{error ?? "Not found."}</Alert>
      </>
    );
  }

  const name = unit.name || "—";

  // Only the count: the list itself is one click away, and asking for a page of
  // people to say "3" would be fetching a screenful to render a number. Null
  // when it could not be read, which is not the same as none.
  const people = await api<PageOf<TenantPerson>>(`/person?unit=${unit.id}&size=1`, { tenant: slug })
    .then((page) => page.totalElements)
    .catch(() => null);

  // The neighbours, in the order the list shows them. Steps rather than a row
  // of every unit: a unit is a record, and a tenant may have a great many —
  // eleven reference lists fit across a screen, fifty branches do not.
  //
  // One window of them, so a tenant past that many simply gets no steps rather
  // than wrong ones: being told "next" and landing somewhere arbitrary is worse
  // than not being offered it.
  const siblings = await api<PageOf<TenantUnit>>(`/organization?size=${NEIGHBOURS_WINDOW}`, {
    tenant: slug,
  })
    .then((page) => page.content)
    .catch(() => []);
  const here = siblings.findIndex((candidate) => candidate.id === unit.id);
  const previous = here > 0 ? siblings[here - 1] : null;
  const next = here >= 0 && here + 1 < siblings.length ? siblings[here + 1] : null;

  return (
    <>
      <Link href={backToList} className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
        ← Back to units
      </Link>

      <Card className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            {/*
              A plain img, as everywhere else: the source is signed, changes
              every render and points at whatever host the bucket is, so there
              is nothing to optimise against.
            */}
            {unit.photoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={unit.photoUrl}
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
              <p className="mt-1 text-sm text-ink-muted">{unit.address || "No address"}</p>
              <p className="text-sm text-ink-muted">{unit.email || "No email"}</p>
            </div>
          </div>

          {/* Owner only, matching who the API lets change a unit, and editing
              stays on the list where the form already lives. */}
          {role === "OWNER" ? (
            <Link
              href={`${backToList}?edit=${unit.id}`}
              className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-surface-muted"
            >
              Edit
            </Link>
          ) : null}
        </div>

        <dl className="mt-6 divide-y divide-line border-t border-line text-sm">
          <Row label="Kind of unit" value={referenceLabel(lists.UNIT_TYPE, unit.unitType)} />
          <Row
            label="Operating status"
            value={referenceLabel(lists.OPERATING_STATUS, unit.operatingStatus)}
          />
          <Row label="Province" value={referenceLabel(lists.PROVINCE, unit.province)} />
          <Row label="Address" value={unit.address} />
          <Row label="Email" value={unit.email} />
        </dl>

        {/* The mirror of the unit row on a person: from this place to the
            people at it, arriving with the filter already applied. */}
        {previous || next ? (
          <nav
            aria-label="The other units"
            className="mt-4 flex items-center justify-between gap-3 border-t border-line pt-4 text-sm"
          >
            {previous ? (
              <Link
                href={`/organizations/${slug}/units/${previous.id}`}
                rel="prev"
                className="min-w-0 truncate text-ink-muted transition hover:text-ink"
              >
                ← {previous.name || "Previous unit"}
              </Link>
            ) : (
              <span />
            )}
            {next ? (
              <Link
                href={`/organizations/${slug}/units/${next.id}`}
                rel="next"
                className="min-w-0 truncate text-right text-ink-muted transition hover:text-ink"
              >
                {next.name || "Next unit"} →
              </Link>
            ) : (
              <span />
            )}
          </nav>
        ) : null}

        <Link
          href={`/organizations/${slug}/people?unit=${unit.id}`}
          className="mt-4 inline-block border-t border-line pt-4 text-sm text-ink-muted transition hover:text-ink"
        >
          {people === null
            ? "People at this unit →"
            : people === 1
              ? "1 person at this unit →"
              : `${people} people at this unit →`}
        </Link>
      </Card>
    </>
  );
}

function Row({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="flex justify-between gap-4 py-3">
      <dt className="shrink-0 text-ink-muted">{label}</dt>
      <dd className="truncate text-right text-ink">{value || "—"}</dd>
    </div>
  );
}
