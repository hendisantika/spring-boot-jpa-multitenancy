import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import type { MemberDetail } from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Member" };

/**
 * One membership, whole. The card on the organization page has room for an
 * address and a role, which leaves the rest — the phone number somebody would
 * actually ring, and since when this person has had access — nowhere to live.
 */
export default async function MemberPage({ params }: PageProps<"/organizations/[slug]/members/[id]">) {
  const { slug, id } = await params;
  const role = await getRole(slug);
  const backToOrganization = `/organizations/${slug}`;

  if (!role) {
    return (
      <>
        <PageHeading title="Member" />
        <Alert>You are not a member of this organization.</Alert>
      </>
    );
  }

  let member: MemberDetail | null = null;
  let error: string | null = null;

  try {
    member = await api<MemberDetail>(`/api/organizations/${slug}/users/${id}`);
  } catch (e) {
    if (e instanceof ApiError) {
      error = e.status === 404 ? "Nobody with that account is a member here." : e.message;
    } else {
      error = "Cannot reach the API.";
    }
  }

  if (error || !member) {
    return (
      <>
        <Link
          href={backToOrganization}
          className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
        >
          ← Back to the organization
        </Link>
        <PageHeading title="Member" />
        <Alert>{error ?? "Not found."}</Alert>
      </>
    );
  }

  return (
    <>
      <Link
        href={backToOrganization}
        className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
      >
        ← Back to the organization
      </Link>

      <Card className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            {/* The same tile as every other detail screen. A plain img: the URL
                is signed and changes on every render. */}
            {member.photoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={member.photoUrl}
                alt=""
                className="size-28 shrink-0 rounded-xl border border-line object-cover"
              />
            ) : (
              <span
                aria-hidden="true"
                className="grid size-28 shrink-0 place-items-center rounded-xl border border-line bg-surface-muted text-3xl font-medium text-ink-muted"
              >
                {member.email.trim().charAt(0).toUpperCase() || "?"}
              </span>
            )}
            <div className="min-w-0">
              <h1 className="text-2xl font-semibold tracking-tight text-ink">{member.email}</h1>
              {/* Worth saying once: this is a membership, not the account. */}
              <p className="mt-1 text-sm text-ink-muted">
                A member of {slug}. One account can belong to several organizations.
              </p>
            </div>
          </div>
          <Badge tone={member.role === "OWNER" ? "brand" : "muted"}>{member.role}</Badge>
        </div>

        <dl className="mt-6 divide-y divide-line border-t border-line text-sm">
          <Row label="Phone" value={member.phoneNumber} />
          <Row label="Email confirmed" value={member.emailVerified ? "Yes" : "Not yet"} />
          <Row label="Joined" value={formatJoined(member.joinedAt)} />
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

/**
 * Null means the membership predates the column, which is unknown rather than
 * never — so it says so instead of falling through to a bare dash that reads
 * like an empty field.
 */
function formatJoined(value: string | null): string {
  if (!value) return "Before this was recorded";
  return new Date(value).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}
