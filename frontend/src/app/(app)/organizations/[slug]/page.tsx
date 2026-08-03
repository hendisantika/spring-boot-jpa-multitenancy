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
  categoryName,
  type Page as PageOf,
  type ReferenceLists,
  type TenantPerson,
  type TenantUnit,
} from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";
import { Avatar } from "@/components/Avatar";
import { Pager } from "@/components/Pager";
import { firstValue } from "@/lib/listing";

/** Enough to recognise the tenant's data without becoming a second list. */
const PREVIEW = 5;

/**
 * Smaller than the tenant lists: this card shares a column with the photo and
 * invite panels, and a page of twenty would push them off the screen.
 */
const MEMBERS_PER_PAGE = 8;

export default async function OrganizationPage({ params, searchParams }: PageProps<"/organizations/[slug]">) {
  const { slug } = await params;
  const { fresh, members: membersPage, memberq } = await searchParams;
  // Their own parameters, because this page carries more than one list and they
  // move independently.
  const page = Math.max(0, Number(firstValue(membersPage)) || 0);
  const query = (firstValue(memberq) ?? "").trim();

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
  let members: PageOf<Member> = { content: [], page: 0, size: MEMBERS_PER_PAGE, totalElements: 0, totalPages: 0 };
  let invitations: Invitation[] = [];
  let error: string | null = null;

  try {
    [organization, members] = await Promise.all([
      api<Organization>(`/api/organizations/${slug}`),
      api<PageOf<Member>>(
        `/api/organizations/${slug}/users?page=${page}&size=${MEMBERS_PER_PAGE}` +
          (query ? `&q=${encodeURIComponent(query)}` : ""),
      ),
    ]);
    // Owner only, so a member's page does not 403 on a panel they never see.
    if (role === "OWNER") {
      invitations = await api<Invitation[]>(`/api/organizations/${slug}/invitations`);
    }
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  // The tenant's own database, fetched apart from the rest: everything above
  // lives centrally, and a tenant database that is unreachable should empty two
  // cards rather than take the profile down with it.
  const [people, units, lists] = await Promise.all([
    api<PageOf<TenantPerson>>(`/person?size=${PREVIEW}`, { tenant: slug }).catch(() => null),
    api<PageOf<TenantUnit>>(`/organization?size=${PREVIEW}`, { tenant: slug }).catch(() => null),
    api<ReferenceLists>("/reference-data", { tenant: slug }).catch(() => null),
  ]);

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
                <span className="text-sm text-ink-muted">{members.totalElements}</span>
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

            {/* A plain GET form: the search is in the address, so a result is
                somewhere you can come back to. Paging resets to the first page,
                because page four of the old search is not page four of this
                one. */}
            <form action={`/organizations/${slug}`} className="mb-4 flex gap-2">
              <input
                type="search"
                name="memberq"
                defaultValue={query}
                placeholder="Search address or role"
                aria-label="Search members"
                className="min-w-0 flex-1 rounded-lg border border-line bg-surface px-3 py-1.5 text-sm text-ink outline-none transition placeholder:text-ink-muted/70 focus:border-brand"
              />
              <button
                type="submit"
                className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-surface-muted"
              >
                Apply
              </button>
            </form>

            <ul className="divide-y divide-line">
              {members.content.map((member) => (
                <MemberRow
                  key={`${member.accountId}-${member.email}`}
                  member={member}
                  slug={slug}
                  canRemove={role === "OWNER"}
                />
              ))}
              {members.totalElements === 0 ? (
                <li className="py-3 text-sm text-ink-muted">
                  {query ? `Nobody here matches "${query}".` : "No one yet."}
                </li>
              ) : null}
            </ul>

            {/* Only its own parameter moves, so the tenant cards below stay
                where they are while somebody walks the membership list. */}
            <Pager
              href={(next) =>
                `/organizations/${slug}?members=${next}` +
                (query ? `&memberq=${encodeURIComponent(query)}` : "")
              }
              page={members.page}
              size={members.size}
              totalElements={members.totalElements}
              totalPages={members.totalPages}
            />
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

      {/* The tenant's own database. Everything above this lives centrally, and
          the two are worth keeping visibly apart. */}
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <TenantCard
          title="People in this tenant"
          seeAll={`/organizations/${slug}/people`}
          total={people?.totalElements ?? null}
        >
          {people?.content.map((person) => {
            const name = [person.firstName, person.lastName].filter(Boolean).join(" ") || "—";
            return (
              <PreviewRow
                key={person.id}
                href={`/organizations/${slug}/people/${person.id}`}
                photoUrl={person.photoUrl}
                initialFrom={name || person.email}
                title={name}
                subtitle={[person.email, person.mobile].filter(Boolean).join(" · ")}
              />
            );
          })}
        </TenantCard>

        <TenantCard
          title="Business units"
          seeAll={`/organizations/${slug}/units`}
          total={units?.totalElements ?? null}
        >
          {units?.content.map((unit) => (
            <PreviewRow
              key={unit.id}
              href={`/organizations/${slug}/units/${unit.id}`}
              photoUrl={unit.photoUrl}
              initialFrom={unit.name}
              title={unit.name || "—"}
              subtitle={[unit.address, unit.email].filter(Boolean).join(" · ")}
              // A place, not a face.
              rounded="lg"
            />
          ))}
        </TenantCard>

        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="font-semibold text-ink">Reference lists</h2>
            <span className="text-sm text-ink-muted">
              {lists ? Object.keys(lists).length : "—"}
            </span>
          </div>
          <p className="mb-4 text-sm text-ink-muted">
            {/* Worth one line: these are the tenant's own copy, not a shared
                catalogue, which is why they are on this page at all. */}
            This tenant&apos;s own copy of the lists behind every dropdown. Renaming a label
            here would not rewrite anybody&apos;s record: a record stores the code.
          </p>

          {lists === null ? (
            <p className="text-sm text-ink-muted">This tenant&apos;s database could not be read.</p>
          ) : (
            <ul className="divide-y divide-line">
              {Object.entries(lists).map(([category, values]) => (
                <li key={category}>
                  <Link
                    href={`/organizations/${slug}/reference-data/${category}`}
                    className="flex items-center justify-between gap-3 py-3 transition hover:opacity-80"
                  >
                    <span className="truncate text-sm text-ink hover:underline">
                      {categoryName(category)}
                    </span>
                    <span className="shrink-0 text-xs text-ink-muted">
                      {values.filter((value) => value.active).length} of {values.length} in use
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}

/**
 * @param total null when the tenant database could not be read, which is not the
 *              same as a tenant with nothing in it and must not read like one
 */
function TenantCard({
  title,
  seeAll,
  total,
  children,
}: {
  title: string;
  seeAll: string;
  total: number | null;
  children: React.ReactNode;
}) {
  const rows = Array.isArray(children) ? children.filter(Boolean) : children;
  const empty = !Array.isArray(rows) || rows.length === 0;

  return (
    <Card className="flex flex-col p-6">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-ink">{title}</h2>
        <span className="text-sm text-ink-muted">{total ?? "—"}</span>
      </div>

      {total === null ? (
        <p className="text-sm text-ink-muted">This tenant&apos;s database could not be read.</p>
      ) : empty ? (
        <p className="text-sm text-ink-muted">Nothing here yet.</p>
      ) : (
        <ul className="divide-y divide-line">{rows}</ul>
      )}

      <Link
        href={seeAll}
        className="mt-4 self-end text-sm text-ink-muted transition hover:text-ink"
      >
        See all →
      </Link>
    </Card>
  );
}

function PreviewRow({
  href,
  photoUrl,
  initialFrom,
  title,
  subtitle,
  rounded,
}: {
  href: string;
  photoUrl: string | null;
  initialFrom: string | null;
  title: string;
  subtitle: string;
  rounded?: "full" | "lg";
}) {
  return (
    <li>
      {/* The whole row is the link: a name on its own is a small target, and
          there is nothing else on the row to click. */}
      <Link href={href} className="flex items-center gap-3 py-3 transition hover:opacity-80">
        <Avatar photoUrl={photoUrl} email={initialFrom} rounded={rounded} />
        <div className="min-w-0">
          <p className="truncate text-sm text-ink">{title}</p>
          <p className="truncate text-xs text-ink-muted">{subtitle || "No details"}</p>
        </div>
      </Link>
    </li>
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
