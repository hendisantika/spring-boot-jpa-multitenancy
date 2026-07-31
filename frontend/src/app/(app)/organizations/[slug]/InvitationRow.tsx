import { revokeInvitation } from "@/app/actions/invitations";
import { Badge } from "@/components/ui";
import type { Invitation } from "@/lib/types";

export function InvitationRow({ invitation, slug }: { invitation: Invitation; slug: string }) {
  const expires = new Date(invitation.expiresAt);

  return (
    <li className="flex items-center justify-between gap-3 py-3">
      <div className="min-w-0">
        <p className="truncate text-sm text-ink">{invitation.email}</p>
        <p className="text-xs text-ink-muted">
          Expires {expires.toLocaleDateString()} at {expires.toLocaleTimeString()}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-2">
        <Badge tone={invitation.role === "OWNER" ? "brand" : "muted"}>{invitation.role}</Badge>
        <form action={revokeInvitation}>
          <input type="hidden" name="slug" value={slug} />
          <input type="hidden" name="invitationId" value={invitation.id} />
          <button
            type="submit"
            className="rounded-md px-2 py-1 text-xs text-ink-muted transition hover:bg-danger/10 hover:text-danger"
          >
            Revoke
          </button>
        </form>
      </div>
    </li>
  );
}
