"use client";

import { useActionState, useState } from "react";

import { inviteMember, type InviteState } from "@/app/actions/invitations";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input, Select } from "@/components/ui";

export function InviteForm({ slug }: { slug: string }) {
  const [state, action] = useActionState<InviteState, FormData>(inviteMember, {});
  const [copied, setCopied] = useState(false);

  async function copyLink() {
    if (!state.acceptUrl) return;
    await navigator.clipboard.writeText(state.acceptUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="slug" value={slug} />

      {state.error ? <Alert>{state.error}</Alert> : null}

      {state.ok && state.acceptUrl ? (
        <div className="rounded-lg border border-brand/30 bg-brand/10 p-3">
          <p className="text-sm font-medium text-ink">Invitation for {state.invitedEmail}</p>
          <p className="mt-1 text-xs text-ink-muted">
            No mail is sent, so pass this link on. It is shown once and cannot be retrieved again.
          </p>
          <div className="mt-2 flex items-center gap-2">
            <code className="min-w-0 flex-1 truncate rounded border border-line bg-surface px-2 py-1.5 font-mono text-xs text-ink">
              {state.acceptUrl}
            </code>
            <button
              type="button"
              onClick={copyLink}
              className="shrink-0 rounded-md border border-line bg-surface px-2.5 py-1.5 text-xs text-ink transition hover:bg-surface-muted"
            >
              {copied ? "Copied" : "Copy"}
            </button>
          </div>
        </div>
      ) : null}

      <Field label="Email">
        <Input
          name="email"
          type="email"
          required
          placeholder="nurse@example.com"
          defaultValue={state.values?.email ?? ""}
        />
      </Field>

      <Field label="Role">
        <Select name="role" defaultValue="MEMBER">
          <option value="MEMBER">Member</option>
          <option value="OWNER">Owner</option>
        </Select>
      </Field>

      <SubmitButton pendingLabel="Creating link…">Send invitation</SubmitButton>
    </form>
  );
}
