import Link from "next/link";

import { InvitationRow } from "./InvitationRow";
import { InviteForm } from "./InviteForm";
import { MemberRow } from "./MemberRow";
import { RefreshSessionNotice } from "./RefreshSessionNotice";
import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import {
  ORG_STRUCTURES,
  PRACTICE_SPECIALITIES,
  labelOf,
  type Invitation,
  type Member,
  type Organization,
} from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export default async function OrganizationPage({ params, searchParams }: PageProps<"/organizations/[slug]">) {
  const { slug } = await params;
  const { fresh } = await searchParams;

  const role = await getRole(slug);

  // A membership created moments ago is not in the current token yet, so the
  // page says so instead of showing a bare 403.
  if (!role) {
    return (
      <>
        <PageHeading title="Almost there" />
        <RefreshSessionNotice slug={slug} justCreated={fresh === "1"} />
      </>
    );
  }

  let organization: Organization | null = null;
  let members: Member[] = [];
  let invitations: Invitation[] = [];
  let error: string | null = null;

  try {
    [organization, members] = await Promise.all([
      api<Organization>(`/api/organizations/${slug}`),
      api<Member[]>(`/api/organizations/${slug}/users`),
    ]);
    // Owner only, so a member's page does not 403 on a panel they never see.
    if (role === "OWNER") {
      invitations = await api<Invitation[]>(`/api/organizations/${slug}/invitations`);
    }
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  if (error || !organization) {
    return (
      <>
        <PageHeading title="Organization" />
        <Alert>{error ?? "Not found."}</Alert>
      </>
    );
  }

  return (
    <>
      <Link href="/dashboard" className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
        ← Back
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-ink">
            {organization.businessName}
          </h1>
          <p className="mt-1 font-mono text-sm text-ink-muted">{organization.subdomain}</p>
        </div>
        <div className="flex items-center gap-2">
          <Badge tone={role === "OWNER" ? "brand" : "muted"}>You are {role}</Badge>
          <Badge>{organization.status}</Badge>
          {role === "OWNER" ? (
            <Link
              href={`/organizations/${slug}/edit`}
              className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-surface-muted"
            >
              Edit
            </Link>
          ) : null}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.1fr_1fr]">
        <Card className="p-6">
          <h2 className="mb-4 font-semibold text-ink">Profile</h2>
          <dl className="space-y-3 text-sm">
            <Row label="Business email" value={organization.businessEmail} />
            <Row
              label="Contact"
              value={[organization.contactFirstName, organization.contactLastName]
                .filter(Boolean)
                .join(" ")}
            />
            <Row label="Job title" value={organization.jobTitle} />
            <Row label="Phone" value={organization.phoneNumber} />
            <Row label="Structure" value={labelOf(ORG_STRUCTURES, organization.orgStructure)} />
            <Row
              label="Speciality"
              value={labelOf(PRACTICE_SPECIALITIES, organization.practiceSpeciality)}
            />
            <Row label="Database" value={organization.databaseName} mono />
          </dl>
        </Card>

        <div className="space-y-6">
          <Card className="p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-semibold text-ink">People</h2>
              <span className="text-sm text-ink-muted">{members.length}</span>
            </div>

            <ul className="divide-y divide-line">
              {members.map((member) => (
                <MemberRow
                  key={`${member.accountId}-${member.email}`}
                  member={member}
                  slug={slug}
                  canRemove={role === "OWNER"}
                />
              ))}
              {members.length === 0 ? (
                <li className="py-3 text-sm text-ink-muted">No one yet.</li>
              ) : null}
            </ul>
          </Card>

          {role === "OWNER" && invitations.length > 0 ? (
            <Card className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="font-semibold text-ink">Pending invitations</h2>
                <span className="text-sm text-ink-muted">{invitations.length}</span>
              </div>
              <ul className="divide-y divide-line">
                {invitations.map((invitation) => (
                  <InvitationRow key={invitation.id} invitation={invitation} slug={slug} />
                ))}
              </ul>
            </Card>
          ) : null}

          {role === "OWNER" ? (
            <Card className="p-6">
              <h2 className="mb-1 font-semibold text-ink">Invite someone</h2>
              <p className="mb-4 text-sm text-ink-muted">
                They choose their own password when they accept, so you never handle it. An existing
                account is granted access rather than duplicated.
              </p>
              <InviteForm slug={slug} />
            </Card>
          ) : (
            <Card className="p-6">
              <p className="text-sm text-ink-muted">
                Only the owner of this organization can add or remove people.
              </p>
            </Card>
          )}
        </div>
      </div>
    </>
  );
}

function Row({ label, value, mono }: { label: string; value?: string | null; mono?: boolean }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="shrink-0 text-ink-muted">{label}</dt>
      <dd className={`truncate text-right text-ink ${mono ? "font-mono text-xs" : ""}`}>
        {value || "—"}
      </dd>
    </div>
  );
}
