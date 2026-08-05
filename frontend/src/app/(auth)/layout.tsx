import Link from "next/link";
import { redirect } from "next/navigation";

import { isSignedIn } from "@/lib/session";

export default async function AuthLayout({ children }: { children: React.ReactNode }) {
  if (await isSignedIn()) redirect("/dashboard");

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center px-4 py-12">
      <Link href="/" className="mb-8 flex items-center gap-2">
        <span className="grid size-8 place-items-center rounded-lg bg-brand text-sm font-bold text-brand-ink">K</span>
        <span className="text-lg font-semibold tracking-tight text-ink">Kliniku</span>
      </Link>
      <div className="w-full max-w-md">{children}</div>
      <p className="mt-8 text-center text-xs text-ink-muted">
        One account reaches every organization you belong to.
      </p>
    </div>
  );
}
