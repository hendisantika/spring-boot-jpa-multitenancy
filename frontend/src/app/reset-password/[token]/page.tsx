import Link from "next/link";

import { ResetPasswordForm } from "./ResetPasswordForm";
import { ApiError, api } from "@/lib/api";
import { Card } from "@/components/ui";

export const metadata = { title: "Choose a new password" };

/**
 * Outside the (auth) group on purpose: that layout sends a signed-in visitor to
 * the dashboard, and a reset link has to work whatever session the browser
 * happens to hold.
 */
export default async function ResetPasswordPage({ params }: PageProps<"/reset-password/[token]">) {
  const { token } = await params;

  let email: string | null = null;
  let error: string | null = null;

  try {
    email = (await api<{ email: string }>(`/api/auth/password/reset/${token}`, { anonymous: true })).email;
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
        <Card className="p-6">
          {error || !email ? (
            <>
              <h1 className="text-xl font-semibold tracking-tight text-ink">
                This link cannot be used
              </h1>
              <p className="mt-1 mb-5 text-sm text-ink-muted">
                {error ?? "The link is not valid."} It may have been used already, replaced by a
                newer one, or expired.
              </p>
              <Link href="/forgot-password" className="text-sm font-medium text-brand hover:underline">
                Ask for a new link
              </Link>
            </>
          ) : (
            <>
              <h1 className="text-xl font-semibold tracking-tight text-ink">Choose a new password</h1>
              <p className="mt-1 mb-6 text-sm text-ink-muted">
                For {email}. Signing in elsewhere will need the new password.
              </p>
              <ResetPasswordForm token={token} />
            </>
          )}
        </Card>
      </div>
    </div>
  );
}
