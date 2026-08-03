"use server";

import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";

import { ApiError, api } from "@/lib/api";
import { saveSession } from "@/lib/session";
import type { CreatedInvitation, FormState, TokenPair } from "@/lib/types";

/**
 * The accept link comes back only when the email did not go out, and is never
 * retrievable again: the backend keeps only a hash of the token. Once the
 * recipient's mailbox has it, the owner has no reason to hold it.
 */
export type InviteState = FormState & {
  acceptUrl?: string | null;
  emailed?: boolean;
  invitedEmail?: string;
};

export async function inviteMember(_prev: InviteState, formData: FormData): Promise<InviteState> {
  const slug = String(formData.get("slug") ?? "");
  const email = String(formData.get("email") ?? "").trim();

  let created: CreatedInvitation;
  try {
    created = await api<CreatedInvitation>(`/api/organizations/${slug}/invitations`, {
      method: "POST",
      json: { email, role: String(formData.get("role") ?? "MEMBER") },
    });
  } catch (error) {
    return { error: messageOf(error), values: { email } };
  }

  revalidatePath(`/organizations/${slug}`);
  return {
    ok: true,
    emailed: created.emailed,
    acceptUrl: created.acceptUrl,
    invitedEmail: created.email,
  };
}

export async function revokeInvitation(formData: FormData) {
  const slug = String(formData.get("slug") ?? "");
  const invitationId = String(formData.get("invitationId") ?? "");

  try {
    await api<void>(`/api/organizations/${slug}/invitations/${invitationId}`, { method: "DELETE" });
  } catch {
    // The page re-renders from the server, so a failure simply shows no change.
  }
  revalidatePath(`/organizations/${slug}`);
  // The detail screen offers this too, and it is the one page that has to show
  // the change: withdrawing from it and still reading PENDING is worse than
  // not offering the button at all.
  revalidatePath(`/organizations/${slug}/invitations/${invitationId}`);
}

/**
 * Accepting signs the recipient in, so they land inside the organization rather
 * than at a login form.
 */
export async function acceptInvitation(_prev: FormState, formData: FormData): Promise<FormState> {
  const token = String(formData.get("token") ?? "");
  const password = String(formData.get("password") ?? "");
  const slug = String(formData.get("slug") ?? "");
  const email = String(formData.get("email") ?? "");

  if (formData.get("needsPassword") === "1" && password !== String(formData.get("confirmPassword") ?? "")) {
    return { error: "The two passwords do not match." };
  }

  try {
    const tokens = await api<TokenPair>(`/api/invitations/${token}/accept`, {
      method: "POST",
      json: { password: password || null },
      anonymous: true,
    });
    await saveSession(tokens, email);
  } catch (error) {
    return { error: messageOf(error) };
  }

  redirect(`/organizations/${slug}`);
}

function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message.includes("fetch failed")) {
    return "Cannot reach the API. Is the backend running on port 8080?";
  }
  return "Something went wrong. Please try again.";
}
