import { cookies } from "next/headers";

import type { TenantRole, TokenPair } from "@/lib/types";

const ACCESS_TOKEN = "mt_access";
const REFRESH_TOKEN = "mt_refresh";
const MEMBERSHIPS = "mt_memberships";
const EMAIL = "mt_email";

/**
 * Tokens live in httpOnly cookies rather than localStorage, so a script on the
 * page cannot read them and every call goes out from the server.
 */
export async function saveSession(tokens: TokenPair, email: string) {
  const store = await cookies();
  const secure = process.env.NODE_ENV === "production";
  const base = { httpOnly: true, sameSite: "lax" as const, secure, path: "/" };

  store.set(ACCESS_TOKEN, tokens.accessToken, { ...base, maxAge: 60 * 30 });
  store.set(REFRESH_TOKEN, tokens.refreshToken, { ...base, maxAge: 60 * 60 * 24 * 14 });
  store.set(MEMBERSHIPS, JSON.stringify(tokens.memberships ?? {}), { ...base, maxAge: 60 * 30 });
  store.set(EMAIL, email, { ...base, maxAge: 60 * 60 * 24 * 14 });
}

export async function clearSession() {
  const store = await cookies();
  for (const name of [ACCESS_TOKEN, REFRESH_TOKEN, MEMBERSHIPS, EMAIL]) {
    store.delete(name);
  }
}

export async function getAccessToken(): Promise<string | null> {
  return (await cookies()).get(ACCESS_TOKEN)?.value ?? null;
}

export async function getRefreshToken(): Promise<string | null> {
  return (await cookies()).get(REFRESH_TOKEN)?.value ?? null;
}

export async function getEmail(): Promise<string | null> {
  return (await cookies()).get(EMAIL)?.value ?? null;
}

/**
 * The tenants this session may reach, as the access token saw them. A membership
 * created after the token was issued only appears once it is refreshed, which is
 * why the UI offers a refresh after registering an organization.
 */
export async function getMemberships(): Promise<Record<string, TenantRole>> {
  const raw = (await cookies()).get(MEMBERSHIPS)?.value;
  if (!raw) return {};
  try {
    return JSON.parse(raw) as Record<string, TenantRole>;
  } catch {
    return {};
  }
}

export async function getRole(slug: string): Promise<TenantRole | null> {
  return (await getMemberships())[slug] ?? null;
}

/**
 * The email an access token was minted for. Neither refreshing nor changing a
 * password carries one in its response, and the cookie may be a fortnight stale
 * now that the address can change, so the token is the thing to read.
 */
export function emailFromAccessToken(accessToken: string): string {
  try {
    const payload = accessToken.split(".")[1];
    const claims = JSON.parse(Buffer.from(payload, "base64url").toString()) as { email?: string };
    return claims.email ?? "";
  } catch {
    return "";
  }
}

export async function isSignedIn(): Promise<boolean> {
  return (await getAccessToken()) !== null;
}
