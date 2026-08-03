import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import { referenceLabel, type ReferenceLists, type TenantUnit } from "@/lib/types";
import { Alert, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Business unit" };

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
