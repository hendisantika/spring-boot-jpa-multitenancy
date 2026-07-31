"use server";

import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";

import { ApiError, api, jsonPart } from "@/lib/api";
import { clearSession, getRefreshToken, saveSession } from "@/lib/session";
import type { Account, FormState, TokenPair } from "@/lib/types";

export async function signUp(_prev: FormState, formData: FormData): Promise<FormState> {
  const email = String(formData.get("email") ?? "").trim();
  const phoneNumber = String(formData.get("phoneNumber") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const photo = formData.get("photo");
  // Handed back so a rejected form re-renders filled in. Never the passwords.
  const values = { email, phoneNumber };

  if (password !== String(formData.get("confirmPassword") ?? "")) {
    return { error: "The two passwords do not match.", values };
  }

  const payload = new FormData();
  payload.append("account", jsonPart({ email, phoneNumber, password }), "account.json");
  if (photo instanceof File && photo.size > 0) {
    payload.append("photo", photo, photo.name);
  }

  try {
    await api<Account>("/api/auth/signup", { method: "POST", body: payload, anonymous: true });
  } catch (error) {
    return { error: messageOf(error), values };
  }

  // Signing up does not sign you in, so go straight through the login it needs.
  return logIn({}, toLoginForm(email, password));
}

export async function logIn(_prev: FormState, formData: FormData): Promise<FormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  try {
    const tokens = await api<TokenPair>("/api/auth/login", {
      method: "POST",
      json: { email, password },
      anonymous: true,
    });
    await saveSession(tokens, email);
  } catch (error) {
    return { error: messageOf(error), values: { email } };
  }

  redirect("/dashboard");
}

/**
 * Exchanges the refresh token for a new pair. Memberships are read from the
 * database at that moment, so this is how an organization registered during the
 * session becomes reachable.
 */
export async function refreshSession(): Promise<FormState> {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) redirect("/login");

  try {
    const tokens = await api<TokenPair>("/api/auth/refresh", {
      method: "POST",
      json: { refreshToken },
      anonymous: true,
    });
    const email = String(formEmailFallback(tokens));
    await saveSession(tokens, email);
  } catch (error) {
    return { error: messageOf(error) };
  }

  revalidatePath("/", "layout");
  return { ok: true };
}

export async function logOut() {
  await clearSession();
  redirect("/login");
}

function toLoginForm(email: string, password: string): FormData {
  const form = new FormData();
  form.set("email", email);
  form.set("password", password);
  return form;
}

/** The refresh response carries no email, so read it out of the token itself. */
function formEmailFallback(tokens: TokenPair): string {
  try {
    const payload = tokens.accessToken.split(".")[1];
    const claims = JSON.parse(Buffer.from(payload, "base64url").toString()) as { email?: string };
    return claims.email ?? "";
  } catch {
    return "";
  }
}

function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message.includes("fetch failed")) {
    return "Cannot reach the API. Is the backend running on port 8080?";
  }
  return "Something went wrong. Please try again.";
}
