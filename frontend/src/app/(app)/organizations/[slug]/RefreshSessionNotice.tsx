"use client";

import { useActionState } from "react";

import { refreshSession } from "@/app/actions/auth";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Card } from "@/components/ui";
import type { FormState } from "@/lib/types";

/**
 * Access tokens carry the tenants the account may reach, so a membership created
 * during this session is not in the current one. Refreshing reads memberships
 * from the database again, which is the intended way to pick it up.
 */
export function RefreshSessionNotice({ slug, justCreated }: { slug: string; justCreated: boolean }) {
  const [state, action] = useActionState<FormState, FormData>(() => refreshSession(), {});

  return (
    <Card className="p-6">
      <h2 className="font-semibold text-ink">
        {justCreated ? `“${slug}” is ready` : "You cannot open this organization yet"}
      </h2>
      <p className="mt-1 text-sm text-ink-muted">
        {justCreated
          ? "Its database and subdomain exist. Your sign-in token was issued before the membership, so it does not carry it yet."
          : "Your current sign-in token does not include a membership for it."}{" "}
        Refresh the session to pick it up.
      </p>

      {state.error ? (
        <div className="mt-4">
          <Alert>{state.error}</Alert>
        </div>
      ) : null}

      <form action={action} className="mt-5">
        <SubmitButton pendingLabel="Refreshing…">Refresh session</SubmitButton>
      </form>
    </Card>
  );
}
