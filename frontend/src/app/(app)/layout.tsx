import Link from "next/link";
import { redirect } from "next/navigation";

import { logOut } from "@/app/actions/auth";
import { getEmail, isSignedIn } from "@/lib/session";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  // Every page below this layout needs a session; checking once here keeps the
  // pages themselves free of the guard.
  if (!(await isSignedIn())) redirect("/login");
  const email = await getEmail();

  return (
    <div className="min-h-dvh">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
          <Link href="/dashboard" className="flex items-center gap-2">
            <span className="grid size-7 place-items-center rounded-lg bg-brand text-xs font-bold text-brand-ink">
              M
            </span>
            <span className="font-semibold tracking-tight text-ink">Multitenancy</span>
          </Link>

          <div className="flex items-center gap-3">
            <span className="hidden text-sm text-ink-muted sm:inline">{email}</span>
            <form action={logOut}>
              <button
                type="submit"
                className="rounded-lg border border-line px-3 py-1.5 text-sm text-ink transition hover:bg-surface-muted"
              >
                Sign out
              </button>
            </form>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8">{children}</main>
    </div>
  );
}
