import Link from "next/link";

import { currentAccount } from "@/lib/account";
import { ApiError, api } from "@/lib/api";
import { getMemberships } from "@/lib/session";
import {
  ORG_STRUCTURES,
  PRACTICE_SPECIALITIES,
  labelOf,
  type Account,
  type Member,
  type Organization,
  type Page as PageOf,
} from "@/lib/types";
import { VerifyEmailBanner } from "./VerifyEmailBanner";
import { Avatar } from "@/components/Avatar";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Your organizations" };

/** How many faces fit on a card before the row starts running the width. */
const FACES = 5;

export default async function DashboardPage() {
  let organizations: Organization[] = [];
  let account: Account | null = null;
  let error: string | null = null;

  try {
    // currentAccount is memoised per render, so the header above has already
    // fetched this and asking again costs nothing.
    [organizations, account] = await Promise.all([
      api<Organization[]>("/api/organizations"),
      currentAccount(),
    ]);
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }
  const memberships = await getMemberships();

  // One call per organization. A dashboard lists the handful somebody belongs
  // to, not a directory, and they go out together — but each catches its own
  // failure, so one unreachable membership list costs that card its faces
  // rather than emptying the page.
  //
  // Only the faces it draws, now that the list is paged: it used to fetch every
  // member of every organization to show five of them, and the total comes from
  // the page rather than from counting what arrived.
  const membersByOrganization = Object.fromEntries(
    await Promise.all(
      organizations.map(async (organization) => [
        organization.slug,
        await api<PageOf<Member>>(
          `/api/organizations/${organization.slug}/users?size=${FACES}`,
        ).catch(() => null),
      ] as const),
    ),
  );

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
                <div className="flex min-w-0 items-center gap-3">
                  {/* Every other list in the app shows the photo; this one, the
                      first screen after signing in, was the last that did not.
                      Square, and falling back to an initial: an organization is
                      a place, and a card with a gap where a logo goes reads as
                      something that failed to load. */}
                  <Avatar
                    photoUrl={organization.photoUrl}
                    email={organization.businessName}
                    rounded="lg"
                  />
                  <div className="min-w-0">
                    <h2 className="truncate font-semibold text-ink">{organization.businessName}</h2>
                    <p className="mt-0.5 truncate text-sm text-ink-muted">{organization.subdomain}</p>
                  </div>
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

              <People members={membersByOrganization[organization.slug]} />
            </Card>
          </Link>
        ))}
      </div>
    </>
  );
}

/**
 * @param members null when that organization's membership list could not be
 *                read, which says nothing about how many people are in it and
 *                so shows nothing rather than "0 people"
 */
function People({ members }: { members: PageOf<Member> | null }) {
  if (!members) return null;

  return (
    <div className="mt-4 flex items-center gap-3 border-t border-line pt-3">
      {/* Overlapped, because these are faces rather than a list: the point is
          who is in there at a glance, and the names are one click away. */}
      <div className="flex -space-x-2">
        {members.content.map((member) => (
          <span key={`${member.accountId}-${member.email}`} className="ring-2 ring-surface rounded-full">
            <Avatar photoUrl={member.photoUrl} email={member.email} />
          </span>
        ))}
      </div>
      {/* The total, not how many faces arrived: five faces beside "7 people" is
          the truth, and counting the page would have said five. */}
      <span className="text-xs text-ink-muted">
        {members.totalElements === 1 ? "1 person" : `${members.totalElements} people`}
      </span>
    </div>
  );
}
