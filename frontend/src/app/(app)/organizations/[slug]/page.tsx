import Link from "next/link";

import { InvitationRow } from "./InvitationRow";
import { InviteForm } from "./InviteForm";
import { MemberRow } from "./MemberRow";
import { OrganizationPhotoForm } from "./OrganizationPhotoForm";
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
        <div className="flex items-center gap-4">
          {/*
            The same tile as the person and unit screens, at the same size: this
            is the detail screen for an organization, and it was showing its
            photo smaller than either of them.

            A signed URL from the API, good for a short while. Not next/image:
            the host is whatever the bucket is configured as, and the URL changes
            every render, so there is nothing to optimise or cache against.
          */}
          {organization.photoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={organization.photoUrl}
              alt=""
              className="size-28 shrink-0 rounded-xl border border-line object-cover"
            />
          ) : (
            // Without this the heading simply slid left and the page looked
            // like one that had never had a photo at all.
            <span
              aria-hidden="true"
              className="grid size-28 shrink-0 place-items-center rounded-xl border border-line bg-surface-muted text-3xl font-medium text-ink-muted"
            >
              {organization.businessName.trim().charAt(0).toUpperCase() || "?"}
            </span>
          )}
          <div className="min-w-0">
            <h1 className="text-2xl font-semibold tracking-tight text-ink">
              {organization.businessName}
            </h1>
            <p className="mt-1 font-mono text-sm text-ink-muted">{organization.subdomain}</p>
          </div>
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

      {/* The tenant's own database, as opposed to everything else on this page,
          which lives centrally. */}
      <div className="mb-6 flex flex-wrap gap-3">
        <Link
          href={`/organizations/${slug}/people`}
          className="rounded-lg border border-line bg-surface px-4 py-2 text-sm text-ink transition hover:bg-surface-muted"
        >
          People in this tenant →
        </Link>
        <Link
          href={`/organizations/${slug}/units`}
          className="rounded-lg border border-line bg-surface px-4 py-2 text-sm text-ink transition hover:bg-surface-muted"
        >
          Business units →
        </Link>
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
            {/* Heading and note together, so the gap before the list is the
                same whether or not the note is there. */}
            <div className="mb-4">
              <div className="flex items-center justify-between">
                <h2 className="font-semibold text-ink">People</h2>
                <span className="text-sm text-ink-muted">{members.length}</span>
              </div>
              {/* Only for a member, and where the missing buttons would have
                  been rather than in a card of its own: a restriction is worth
                  a line, not a panel. An owner has the invite form below. */}
              {role === "OWNER" ? null : (
                <p className="mt-1 text-sm text-ink-muted">
                  Only the owner of this organization can add or remove people.
                </p>
              )}
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

          {role === "OWNER" ? (
            <Card className="p-6">
              <h2 className="font-semibold text-ink">Photo</h2>
              <p className="mt-1 mb-4 text-sm text-ink-muted">
                {/* Said here because the profile form also carries a photo field. */}
                Only the photo. The rest of the profile is behind Edit, so changing a picture does
                not mean re-submitting eight fields that were fine as they were.
              </p>
              <OrganizationPhotoForm slug={slug} photoUrl={organization.photoUrl} />
            </Card>
          ) : null}

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
          ) : null}
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
