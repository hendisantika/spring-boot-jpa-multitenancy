import Link from "next/link";

import { ApiError, api } from "@/lib/api";
import { Card } from "@/components/ui";

export const metadata = { title: "Confirm your email" };

/**
 * Open, and outside the (auth) group: the token in the link is the only
 * credential, and it has to work whatever session the browser holds.
 */
export default async function VerifyEmailPage({ params }: PageProps<"/verify-email/[token]">) {
  const { token } = await params;

  let email: string | null = null;
  let error: string | null = null;

  try {
    email = (await api<{ email: string }>(`/api/auth/verify-email/${token}`, {
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
                newer one, or expired. Sign in and ask for another.
              </p>
            </>
          ) : (
            <>
              <h1 className="text-xl font-semibold tracking-tight text-ink">Email confirmed</h1>
              <p className="mt-1 mb-5 text-sm text-ink-muted">
                {email} is verified. You can register an organization now.
              </p>
            </>
          )}
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
