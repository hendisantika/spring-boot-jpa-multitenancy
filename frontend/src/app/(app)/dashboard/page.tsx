import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { getMemberships } from "@/lib/session";
import {
  ORG_STRUCTURES,
  PRACTICE_SPECIALITIES,
  labelOf,
  type Account,
  type Organization,
} from "@/lib/types";
import { VerifyEmailBanner } from "./VerifyEmailBanner";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Your organizations" };

export default async function DashboardPage() {
  let organizations: Organization[] = [];
  let account: Account | null = null;
  let error: string | null = null;

  try {
    [organizations, account] = await Promise.all([
      api<Organization[]>("/api/organizations"),
      api<Account>("/api/auth/me"),
    ]);
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }
  const memberships = await getMemberships();

  return (
    <>
      {account && !account.emailVerified ? <VerifyEmailBanner email={account.email} /> : null}

      <div className="flex flex-wrap items-start justify-between gap-3">
        <PageHeading
          title="Your organizations"
          description="Only the ones you belong to. Each has its own database and subdomain."
        />
        <Link
          href="/organizations/new"
          className="rounded-lg bg-brand px-4 py-2 text-sm font-medium text-brand-ink transition hover:opacity-90"
        >
          Register organization
        </Link>
      </div>

      {error ? <Alert>{error}</Alert> : null}

      {!error && organizations.length === 0 ? (
        <Card className="p-10 text-center">
          <p className="text-sm font-medium text-ink">Nothing here yet</p>
          <p className="mx-auto mt-1 max-w-sm text-sm text-ink-muted">
            Register an organization and a MySQL database plus a subdomain are created for it, without
            a restart.
          </p>
          <Link
            href="/organizations/new"
            className="mt-5 inline-flex rounded-lg bg-brand px-4 py-2 text-sm font-medium text-brand-ink transition hover:opacity-90"
          >
            Register your first organization
          </Link>
        </Card>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2">
        {organizations.map((organization) => (
          <Link key={organization.slug} href={`/organizations/${organization.slug}`} className="group">
            <Card className="h-full p-5 transition group-hover:border-brand/40 group-hover:shadow-md">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <h2 className="truncate font-semibold text-ink">{organization.businessName}</h2>
                  <p className="mt-0.5 truncate text-sm text-ink-muted">{organization.subdomain}</p>
                </div>
                {memberships[organization.slug] ? (
                  <Badge tone={memberships[organization.slug] === "OWNER" ? "brand" : "muted"}>
                    {memberships[organization.slug]}
                  </Badge>
                ) : null}
              </div>

              <dl className="mt-4 space-y-1.5 text-sm">
                <div className="flex justify-between gap-3">
                  <dt className="text-ink-muted">Structure</dt>
                  <dd className="truncate text-ink">
                    {labelOf(ORG_STRUCTURES, organization.orgStructure)}
                  </dd>
                </div>
                <div className="flex justify-between gap-3">
                  <dt className="text-ink-muted">Speciality</dt>
                  <dd className="truncate text-ink">
                    {labelOf(PRACTICE_SPECIALITIES, organization.practiceSpeciality)}
                  </dd>
                </div>
                <div className="flex justify-between gap-3">
                  <dt className="text-ink-muted">Database</dt>
                  <dd className="truncate font-mono text-xs text-ink">{organization.databaseName}</dd>
                </div>
              </dl>
            </Card>
          </Link>
        ))}
      </div>
    </>
  );
}
