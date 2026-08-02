import { cache } from "react";

import { api } from "@/lib/api";
import type { Account } from "@/lib/types";

/**
 * The signed-in account, fetched once per render however many places ask.
 *
 * The header wants it for the avatar and the dashboard wants it for the
 * verification banner; without `cache` that is two calls to the API for one
 * page. It cannot be held in a cookie instead: `photoUrl` is a signed URL that
 * expires, so it has to be fresh.
 *
 * Answers null rather than throwing. This is called from the layout, so a
 * failure here would take down every page under it — and the account is a
 * decoration on all of them but the dashboard, which reports the failure
 * itself.
 */
export const currentAccount = cache(async (): Promise<Account | null> => {
  try {
    return await api<Account>("/api/auth/me");
  } catch {
    return null;
  }
});
