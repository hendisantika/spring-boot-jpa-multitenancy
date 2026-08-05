import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { Card } from "@/components/ui";

export const metadata = { title: "Confirm your new email" };

/**
 * Open, and outside the (auth) group, for the same reason as verification: the
 * token in the link is the only credential, and whoever opens it is reading the
 * new mailbox, which may be a browser holding no session at all.
 */
export default async function ConfirmEmailPage({ params }: PageProps<"/confirm-email/[token]">) {
  const { token } = await params;

  let email: string | null = null;
  let error: string | null = null;

  try {
    email = (await api<{ email: string }>(`/api/auth/email-change/${token}`, {
      method: "POST",
      anonymous: true,
    })).email;
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
        <Card className="p-6 text-center">
          {error || !email ? (
            <>
              <h1 className="text-xl font-semibold tracking-tight text-ink">
                This link cannot be used
              </h1>
              <p className="mt-1 mb-5 text-sm text-ink-muted">
                {error ?? "The link is not valid."} It may have been used already, replaced by a
                newer one, or expired. Nothing has changed: sign in as before and ask again.
              </p>
            </>
          ) : (
            <>
              <h1 className="text-xl font-semibold tracking-tight text-ink">Email changed</h1>
              <p className="mt-1 mb-5 text-sm text-ink-muted">
                {/* Said outright, because the old address stops working now. */}
                Sign in as {email} from now on. The old address no longer works.
              </p>
            </>
          )}
          {/* Signed in, this lands on the dashboard; read in the new mailbox on
              some other browser, the dashboard sends it to the sign-in page. */}
          <Link
            href="/dashboard"
            className="inline-flex rounded-lg bg-brand px-4 py-2 text-sm font-medium text-brand-ink transition hover:opacity-90"
          >
            Continue
          </Link>
        </Card>
      </div>
    </div>
  );
}
