import { removeMember } from "@/app/actions/organizations";
import { Badge } from "@/components/ui";
import type { Member } from "@/lib/types";

export function MemberRow({
  member,
  slug,
  canRemove,
}: {
  member: Member;
  slug: string;
  canRemove: boolean;
}) {
  return (
    <li className="flex items-center justify-between gap-3 py-3">
      <div className="min-w-0">
        <p className="truncate text-sm text-ink">{member.email}</p>
      </div>
      <div className="flex shrink-0 items-center gap-2">
        <Badge tone={member.role === "OWNER" ? "brand" : "muted"}>{member.role}</Badge>
        {/* The backend refuses to remove the owner, so the control is not offered. */}
        {canRemove && member.role !== "OWNER" && member.accountId ? (
          <form action={removeMember}>
            <input type="hidden" name="slug" value={slug} />
            <input type="hidden" name="accountId" value={member.accountId} />
            <button
              type="submit"
              className="rounded-md px-2 py-1 text-xs text-ink-muted transition hover:bg-danger/10 hover:text-danger"
            >
              Remove
            </button>
          </form>
        ) : null}
      </div>
    </li>
  );
}
