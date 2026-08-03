import Link from "next/link";

import { revokeInvitation } from "@/app/actions/invitations";
import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import type { InvitationDetail } from "@/lib/types";
import { Alert, Badge, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Invitation" };

/**
 * One invitation, whole. The row on the organization page has the address, the
 * role and a date, which leaves out who sent it, when, whether the address is
 * already registered, and — for a pending one past its date — that it has
 * quietly stopped working.
 */
export default async function InvitationPage({ params }: PageProps<"/organizations/[slug]/invitations/[id]">) {
  const { slug, id } = await params;
  const role = await getRole(slug);
  const backToOrganization = `/organizations/${slug}`;

  // Owner only, matching the endpoint. Shown rather than fetched: the API
  // refuses a member anyway, so there is no point loading it to be told.
  if (role !== "OWNER") {
    return (
      <>
        <PageHeading title="Invitation" />
        <Alert>Only the owner of this organization can see its invitations.</Alert>
      </>
    );
  }

  let invitation: InvitationDetail | null = null;
  let error: string | null = null;

  try {
    invitation = await api<InvitationDetail>(`/api/organizations/${slug}/invitations/${id}`);
  } catch (e) {
    if (e instanceof ApiError) {
      error = e.status === 404 ? "No invitation here with that id." : e.message;
    } else {
      error = "Cannot reach the API.";
    }
  }

  if (error || !invitation) {
    return (
      <>
        <Link
          href={backToOrganization}
          className="mb-4 inline-block text-sm text-ink-muted hover:text-ink"
        >
          ← Back to the organization
        </Link>
        <PageHeading title="Invitation" />
        <Alert>{error ?? "Not found."}</Alert>
      </>
    );
  }

  // Expired is not a separate status: the row is still PENDING and still sits
  // in the list, so withdrawing it is how an owner clears it. The badge says
  // expired; the button stays.
  const withdrawable = invitation.status === "PENDING";
  const live = withdrawable && !invitation.expired;

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
          <div className="flex min-w-0 items-center gap-4">
            {/* Only when that address is already registered; otherwise there is
                no account and no face to show. */}
            {invitation.photoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={invitation.photoUrl}
                alt=""
                className="size-28 shrink-0 rounded-xl border border-line object-cover"
              />
            ) : (
              <span
                aria-hidden="true"
                className="grid size-28 shrink-0 place-items-center rounded-xl border border-line bg-surface-muted text-3xl font-medium text-ink-muted"
              >
                {invitation.email.trim().charAt(0).toUpperCase() || "?"}
              </span>
            )}
            <div className="min-w-0">
            <h1 className="text-2xl font-semibold tracking-tight text-ink">{invitation.email}</h1>
            <p className="mt-1 text-sm text-ink-muted">
              {invitation.accountExists
                ? "That address already has an account, so accepting grants it access rather than creating another."
                : "No account has that address yet, so accepting creates one and they choose the password."}
            </p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Badge tone={invitation.role === "OWNER" ? "brand" : "muted"}>{invitation.role}</Badge>
            {/* Expired is not a status the row carries: nothing sweeps them, so
                a PENDING one past its date still says PENDING to the database
                while being useless to whoever holds the link. */}
            <Badge tone={live ? "brand" : "muted"}>
              {invitation.expired && invitation.status === "PENDING" ? "EXPIRED" : invitation.status}
            </Badge>
          </div>
        </div>

        <dl className="mt-6 divide-y divide-line border-t border-line text-sm">
          <Row label="Invited by" value={invitation.invitedBy} />
          <Row label="Sent" value={formatMoment(invitation.createdAt)} />
          <Row label={invitation.expired ? "Expired" : "Expires"} value={formatMoment(invitation.expiresAt)} />
          {invitation.acceptedAt ? (
            <Row label="Accepted" value={formatMoment(invitation.acceptedAt)} />
          ) : null}
        </dl>

        <p className="mt-4 text-xs text-ink-muted">
          {/* Owners will look for the link, so say where it is rather than
              leaving them hunting for a button that cannot exist. */}
          The accept link is not shown here and cannot be: only a hash of it is stored, so it exists
          in {invitation.email}&apos;s mailbox and nowhere else. Withdraw this one and send another
          if it needs replacing.
        </p>

        {withdrawable ? (
          <form action={revokeInvitation} className="mt-4 flex justify-end border-t border-line pt-4">
            <input type="hidden" name="slug" value={slug} />
            <input type="hidden" name="invitationId" value={invitation.id} />
            <button
              type="submit"
              className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-danger/10 hover:text-danger"
            >
              Withdraw
            </button>
          </form>
        ) : null}
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

/** These are moments, not dates: an expiry an hour from now matters. */
function formatMoment(value: string | null): string | null {
  if (!value) return null;
  return new Date(value).toLocaleString("en-GB", {
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
