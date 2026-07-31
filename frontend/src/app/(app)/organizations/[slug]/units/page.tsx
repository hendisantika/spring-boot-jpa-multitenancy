import Link from "next/link";

import { UnitForm } from "./UnitForm";
import { deleteUnit } from "@/app/actions/tenant-data";
import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import type { TenantUnit } from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Business units" };

/**
 * The backend calls these organizations, which collides with the organization
 * that owns the tenant, so the UI does not.
 */
export default async function UnitsPage({ params, searchParams }: PageProps<"/organizations/[slug]/units">) {
  const { slug } = await params;
  const { edit } = await searchParams;
  const role = await getRole(slug);

  if (!role) {
    return (
      <>
        <PageHeading title="Business units" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  let units: TenantUnit[] = [];
  let error: string | null = null;

  try {
    units = await api<TenantUnit[]>("/organization", { tenant: slug });
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  const editingId = typeof edit === "string" ? Number(edit) : null;
  const editing = units.find((unit) => unit.id === editingId) ?? null;

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
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold text-ink">Units</h2>
            <span className="text-sm text-ink-muted">{units.length}</span>
          </div>

          <ul className="divide-y divide-line">
            {units.map((unit) => (
              <li key={unit.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm text-ink">{unit.name || "—"}</p>
                  <p className="truncate text-xs text-ink-muted">
                    {[unit.address, unit.email].filter(Boolean).join(" · ") || "No details"}
                  </p>
                </div>
                {role === "OWNER" ? (
                  <div className="flex shrink-0 items-center gap-1">
                    <Link
                      href={`/organizations/${slug}/units?edit=${unit.id}`}
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
            {units.length === 0 && !error ? (
              <li className="py-3 text-sm text-ink-muted">Nothing yet.</li>
            ) : null}
          </ul>
        </Card>

        {/* Not offered to a member, because the API refuses the write anyway. */}
        {role === "OWNER" ? (
          <Card className="p-6">
            <h2 className="mb-4 font-semibold text-ink">{editing ? "Edit unit" : "Add a unit"}</h2>
            <UnitForm slug={slug} editing={editing} />
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
