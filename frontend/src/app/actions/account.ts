"use server";

import { revalidatePath } from "next/cache";

import { ApiError, api } from "@/lib/api";
import { emailFromAccessToken, saveSession } from "@/lib/session";
import type { Account, FormState, TokenPair } from "@/lib/types";

/**
 * confirmUrl comes back only when mail delivery is off, so the flow stays
 * followable locally. It is the same shape the forgot-password screen uses.
 */
export type EmailChangeState = FormState & { message?: string; confirmUrl?: string | null };

/**
 * Asks to move the account to a different address. Nothing changes yet: the
 * account keeps signing in as it does until the link sent to the new mailbox is
 * opened, so a typo here is a wasted email rather than a lost account.
 */
export async function requestEmailChange(
  _prev: EmailChangeState,
  formData: FormData,
): Promise<EmailChangeState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  // Handed back so a refusal re-renders filled in. Never the password.
  const values = { email };

  if (!email) return { error: "Enter the new address.", values };
  if (!password) return { error: "Enter your current password.", values };

  try {
    const response = await api<{ message: string; confirmUrl: string | null }>(
      "/api/auth/me/email",
      { method: "POST", json: { email, password } },
    );
    revalidatePath("/account");
    return { ok: true, message: response.message, confirmUrl: response.confirmUrl };
  } catch (error) {
    return { ...failure(error), values };
  }
}

/**
 * The password, from inside a session rather than through a reset link.
 *
 * The response is a fresh token pair, and saving it is not optional: the change
 * disowns every refresh token issued before it, this session's included, so
 * skipping this would sign you out for having tidied up your own password.
 */
export async function saveAccountPassword(
  _prev: FormState,
  formData: FormData,
): Promise<FormState> {
  const currentPassword = String(formData.get("currentPassword") ?? "");
  const newPassword = String(formData.get("newPassword") ?? "");

  if (!currentPassword) return { error: "Enter your current password." };
  if (newPassword.length < 8) return { error: "Choose a password of at least 8 characters." };
  if (newPassword !== String(formData.get("confirmPassword") ?? "")) {
    return { error: "The two new passwords do not match." };
  }

  try {
    const tokens = await api<TokenPair>("/api/auth/me/password", {
      method: "PUT",
      json: { currentPassword, newPassword },
    });
    await saveSession(tokens, emailFromAccessToken(tokens.accessToken));
  } catch (error) {
    return failure(error);
  }

  revalidatePath("/account");
  return { ok: true };
}

/**
 * The phone number, which signup asked for and nothing could correct. It goes
 * straight through, unlike the address: nothing signs in with it and nothing is
 * sent to it, so there is nothing to confirm first.
 */
export async function saveAccountPhone(_prev: FormState, formData: FormData): Promise<FormState> {
  const phoneNumber = String(formData.get("phoneNumber") ?? "").trim();
  const values = { phoneNumber };

  if (!phoneNumber) return { error: "Enter a phone number.", values };

  try {
    await api<Account>("/api/auth/me/phone", { method: "PUT", json: { phoneNumber } });
  } catch (error) {
    return { ...failure(error), values };
  }

  revalidatePath("/account");
  return { ok: true, values };
}

/** Drops the outstanding request, rather than waiting a day for it to lapse. */
export async function cancelEmailChange(): Promise<FormState> {
  try {
    await api("/api/auth/me/email", { method: "DELETE" });
  } catch (error) {
    return failure(error);
  }
  revalidatePath("/account");
  return { ok: true };
}

function failure(error: unknown): FormState {
  if (error instanceof ApiError) return { error: error.message };
  if (error instanceof Error && error.message.includes("fetch failed")) {
    return { error: "Cannot reach the API." };
  }
  return { error: "Something went wrong. Please try again." };
}

/**
 * The photo on your own account.
 */
export async function saveAccountPhoto(_prev: FormState, formData: FormData): Promise<FormState> {
  const photo = formData.get("photo");
  const removing = formData.get("removePhoto") === "true";

  if (!(photo instanceof File && photo.size > 0) && !removing) {
    return { error: "Choose a photo, or tick the box to remove the current one." };
  }

  const payload = new FormData();
  if (photo instanceof File && photo.size > 0) {
    payload.append("photo", photo, photo.name);
  } else {
    payload.append("removePhoto", "true");
  }

  try {
    await api<Account>("/api/auth/me/photo", { method: "PUT", body: payload });
  } catch (error) {
    return failure(error);
  }

  // The header on every page shows this, so the whole tree is stale, not just
  // this route.
  revalidatePath("/", "layout");
  return { ok: true };
}
