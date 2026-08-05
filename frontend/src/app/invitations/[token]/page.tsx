import Link from "next/link";

import { AcceptForm } from "./AcceptForm";
import { ApiError, api } from "@/lib/api";
import type { InvitationPreview } from "@/lib/types";
import { Badge, Card } from "@/components/ui";

export const metadata = { title: "Invitation" };

/**
 * Open on purpose: whoever holds the link has not signed in yet, and the token
 * is the only credential.
 */
export default async function InvitationPage({ params }: PageProps<"/invitations/[token]">) {
  const { token } = await params;

  let invitation: InvitationPreview | null = null;
  let error: string | null = null;

  try {
    invitation = await api<InvitationPreview>(`/api/invitations/${token}`, { anonymous: true });
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center px-4 py-12">
      <Link href="/" className="mb-8 flex items-center gap-2">
        <span className="grid size-8 place-items-center rounded-lg bg-brand text-sm font-bold text-brand-ink">K</span>
        <span className="text-lg font-semibold tracking-tight text-ink">Kliniku</span>
      </Link>

      <div className="w-full max-w-md">
        {error || !invitation ? (
          <Card className="p-6">
            <h1 className="text-xl font-semibold tracking-tight text-ink">This link cannot be used</h1>
            <p className="mt-1 mb-5 text-sm text-ink-muted">
              {error ?? "The invitation is not valid."} It may have been used already, withdrawn, or expired.
              Ask whoever invited you to send a new one.
            </p>
            <Link href="/login" className="text-sm font-medium text-brand hover:underline">
              Go to sign in
            </Link>
          </Card>
        ) : (
          <Card className="p-6">
            <div className="mb-4 flex items-start justify-between gap-3">
              <div>
                <h1 className="text-xl font-semibold tracking-tight text-ink">
                  Join {invitation.organizationName}
                </h1>
                <p className="mt-1 text-sm text-ink-muted">{invitation.email}</p>
              </div>
              <Badge tone={invitation.role === "OWNER" ? "brand" : "muted"}>{invitation.role}</Badge>
            </div>

            <p className="mb-5 text-sm text-ink-muted">
              {invitation.accountExists
                ? "You already have an account, so this just adds the organization to it."
                : "Choose a password. Nobody else, including whoever invited you, ever sees it."}
            </p>

            <AcceptForm
              token={token}
              slug={invitation.tenantSlug}
              email={invitation.email}
              needsPassword={!invitation.accountExists}
            />

            <p className="mt-5 text-center text-xs text-ink-muted">
              Expires {new Date(invitation.expiresAt).toLocaleDateString()}
            </p>
          </Card>
        )}
      </div>
    </div>
  );
}
