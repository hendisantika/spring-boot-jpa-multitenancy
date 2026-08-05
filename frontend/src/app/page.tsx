import Link from "next/link";

import { isSignedIn } from "@/lib/session";

export const metadata = {
  title: "Clinic software, multi-tenant by design",
  description:
    "Run every clinic from one account. Each gets its own database and its own subdomain, live the moment you register — then add your team, people and units.",
};

/**
 * The public landing page at the central host. Tenant subdomains never reach it:
 * the Next proxy rewrites their root to the clinic's own pages, so this renders
 * only on the app host (dev.jvm.my.id).
 */
export default async function Home() {
  const signedIn = await isSignedIn();

  return (
    <div className="min-h-dvh">
      <header className="border-b border-line bg-surface/80 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
          <Link href="/" className="flex items-center gap-2">
            <span className="grid size-8 place-items-center rounded-lg bg-brand text-sm font-bold text-brand-ink">
              M
            </span>
            <span className="text-lg font-semibold tracking-tight text-ink">Multitenancy</span>
          </Link>

          <nav className="flex items-center gap-2">
            {signedIn ? (
              <Link
                href="/dashboard"
                className="inline-flex items-center rounded-lg bg-brand px-4 py-2 text-sm font-medium text-brand-ink transition hover:opacity-90"
              >
                Go to dashboard
              </Link>
            ) : (
              <>
                <Link
                  href="/login"
                  className="rounded-lg px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-muted"
                >
                  Sign in
                </Link>
                <Link
                  href="/signup"
                  className="inline-flex items-center rounded-lg bg-brand px-4 py-2 text-sm font-medium text-brand-ink transition hover:opacity-90"
                >
                  Get started
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main>
        {/* Hero */}
        <section className="mx-auto max-w-5xl px-4 pt-16 pb-12 sm:pt-24 sm:pb-16">
          <div className="mx-auto max-w-2xl text-center">
            <span className="inline-flex items-center rounded-full bg-brand/12 px-3 py-1 text-xs font-medium text-brand">
              Clinic management, multi-tenant by design
            </span>
            <h1 className="mt-5 text-4xl font-bold tracking-tight text-ink text-balance sm:text-5xl">
              Run every clinic from one account — each on its own subdomain.
            </h1>
            <p className="mx-auto mt-5 max-w-xl text-base text-ink-muted text-pretty sm:text-lg">
              Register a clinic and it gets its own database and its own address, live in seconds:{" "}
              <span className="font-medium text-ink">yourclinic.jvm.my.id</span>. Add your team, your
              people and your units — one login reaches every clinic you belong to.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <Link
                href={signedIn ? "/dashboard" : "/signup"}
                className="inline-flex items-center rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-brand-ink transition hover:opacity-90"
              >
                {signedIn ? "Go to dashboard" : "Start free"}
              </Link>
              <Link
                href={signedIn ? "/organizations/new" : "/login"}
                className="inline-flex items-center rounded-lg border border-line bg-surface px-5 py-2.5 text-sm font-medium text-ink transition hover:bg-surface-muted"
              >
                {signedIn ? "Register a clinic" : "Sign in"}
              </Link>
            </div>
          </div>

          {/* A browser frame hinting at the per-clinic subdomain. */}
          <div className="mx-auto mt-14 max-w-3xl">
            <div className="overflow-hidden rounded-xl border border-line bg-surface shadow-sm">
              <div className="flex items-center gap-2 border-b border-line bg-surface-muted px-4 py-2.5">
                <span className="size-2.5 rounded-full bg-line" />
                <span className="size-2.5 rounded-full bg-line" />
                <span className="size-2.5 rounded-full bg-line" />
                <span className="ml-3 truncate rounded-md bg-surface px-3 py-1 text-xs text-ink-muted">
                  https://sehat.jvm.my.id
                </span>
              </div>
              <div className="grid gap-4 p-6 sm:grid-cols-3">
                <div className="sm:col-span-1">
                  <div className="flex items-center gap-2">
                    <span className="grid size-9 place-items-center rounded-lg bg-brand/12 text-sm font-bold text-brand">
                      S
                    </span>
                    <div>
                      <p className="text-sm font-semibold text-ink">Klinik Sehat</p>
                      <p className="text-xs text-ink-muted">sehat.jvm.my.id</p>
                    </div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3 sm:col-span-2">
                  {[
                    ["People", "128"],
                    ["Units", "4"],
                    ["Members", "9"],
                    ["Status", "Active"],
                  ].map(([label, value]) => (
                    <div key={label} className="rounded-lg border border-line bg-surface-muted px-3 py-2">
                      <p className="text-xs text-ink-muted">{label}</p>
                      <p className="text-sm font-semibold text-ink">{value}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features */}
        <section className="border-t border-line bg-surface">
          <div className="mx-auto max-w-5xl px-4 py-16">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-2xl font-semibold tracking-tight text-ink sm:text-3xl">
                Everything a clinic needs, kept apart
              </h2>
              <p className="mt-3 text-base text-ink-muted text-pretty">
                Each clinic is a tenant of its own — separate data, separate address — while one
                account reaches all of them.
              </p>
            </div>

            <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map((f) => (
                <div key={f.title} className="rounded-xl border border-line bg-surface p-6 shadow-sm">
                  <span className="grid size-10 place-items-center rounded-lg bg-brand/12 text-brand">
                    {f.icon}
                  </span>
                  <h3 className="mt-4 text-base font-semibold text-ink">{f.title}</h3>
                  <p className="mt-1.5 text-sm text-ink-muted text-pretty">{f.body}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* How it works */}
        <section className="border-t border-line">
          <div className="mx-auto max-w-5xl px-4 py-16">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-2xl font-semibold tracking-tight text-ink sm:text-3xl">
                Live in three steps
              </h2>
            </div>
            <ol className="mx-auto mt-12 grid max-w-3xl gap-6 sm:grid-cols-3">
              {STEPS.map((s, i) => (
                <li key={s.title} className="rounded-xl border border-line bg-surface p-6 shadow-sm">
                  <span className="grid size-8 place-items-center rounded-full bg-brand text-sm font-bold text-brand-ink">
                    {i + 1}
                  </span>
                  <h3 className="mt-4 text-base font-semibold text-ink">{s.title}</h3>
                  <p className="mt-1.5 text-sm text-ink-muted text-pretty">{s.body}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* Call to action */}
        <section className="border-t border-line bg-surface">
          <div className="mx-auto max-w-5xl px-4 py-16">
            <div className="rounded-2xl border border-line bg-brand/8 px-6 py-12 text-center">
              <h2 className="text-2xl font-semibold tracking-tight text-ink sm:text-3xl text-balance">
                Start your clinic in minutes
              </h2>
              <p className="mx-auto mt-3 max-w-md text-sm text-ink-muted text-pretty">
                Create an account, register your clinic, and it is reachable at its own subdomain the
                moment you finish.
              </p>
              <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
                <Link
                  href={signedIn ? "/organizations/new" : "/signup"}
                  className="inline-flex items-center rounded-lg bg-brand px-5 py-2.5 text-sm font-medium text-brand-ink transition hover:opacity-90"
                >
                  {signedIn ? "Register a clinic" : "Create your account"}
                </Link>
                {!signedIn && (
                  <Link
                    href="/login"
                    className="inline-flex items-center rounded-lg border border-line bg-surface px-5 py-2.5 text-sm font-medium text-ink transition hover:bg-surface-muted"
                  >
                    Sign in
                  </Link>
                )}
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-line bg-surface">
        <div className="mx-auto flex max-w-5xl flex-col items-center justify-between gap-3 px-4 py-8 sm:flex-row">
          <div className="flex items-center gap-2">
            <span className="grid size-6 place-items-center rounded-md bg-brand text-xs font-bold text-brand-ink">
              M
            </span>
            <span className="text-sm text-ink-muted">
              Multitenancy — clinic software, one account per team.
            </span>
          </div>
          <div className="flex items-center gap-4 text-sm text-ink-muted">
            <Link href="/login" className="transition hover:text-ink">
              Sign in
            </Link>
            <Link href="/signup" className="transition hover:text-ink">
              Get started
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}

/** Small stroked icons, sized by the wrapper; currentColor picks up the brand. */
const icon = "size-5";

const FEATURES: { title: string; body: string; icon: React.ReactNode }[] = [
  {
    title: "Its own subdomain",
    body: "Register a clinic and yourclinic.jvm.my.id answers for it at once — no per-tenant setup, no restart.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className={icon}>
        <circle cx="12" cy="12" r="9" />
        <path d="M3 12h18M12 3c2.5 2.5 3.8 5.6 3.8 9s-1.3 6.5-3.8 9c-2.5-2.5-3.8-5.6-3.8-9S9.5 5.5 12 3Z" />
      </svg>
    ),
  },
  {
    title: "A database each",
    body: "Every clinic's records live in a database provisioned for it, so one tenant's data never mixes with another's.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className={icon}>
        <ellipse cx="12" cy="5.5" rx="7" ry="3" />
        <path d="M5 5.5v13c0 1.7 3.1 3 7 3s7-1.3 7-3v-13M5 12c0 1.7 3.1 3 7 3s7-1.3 7-3" />
      </svg>
    ),
  },
  {
    title: "People and units",
    body: "Keep your staff and your locations in one place, each with a photo, searchable and filterable.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className={icon}>
        <circle cx="9" cy="8" r="3.2" />
        <path d="M3.5 20a5.5 5.5 0 0 1 11 0M16 6.5a3 3 0 0 1 0 6M17.5 20a5.2 5.2 0 0 0-3-4.7" />
      </svg>
    ),
  },
  {
    title: "Roles and invitations",
    body: "Owners and members, and an invite by email — the person sets their own password when they accept.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className={icon}>
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="m3.5 7 8.5 6 8.5-6" />
      </svg>
    ),
  },
  {
    title: "Your own lookups",
    body: "Reference data — blood types, document types, whatever a clinic needs — is the tenant's to define and version.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className={icon}>
        <path d="M4 6h16M4 12h16M4 18h10" />
        <circle cx="19" cy="18" r="1.6" fill="currentColor" stroke="none" />
      </svg>
    ),
  },
  {
    title: "Secure by default",
    body: "Signed-in access, verified email before a clinic is created, and photos served through short-lived signed links.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className={icon}>
        <path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3Z" />
        <path d="m9 12 2 2 4-4" />
      </svg>
    ),
  },
];

const STEPS: { title: string; body: string }[] = [
  {
    title: "Create your account",
    body: "One account for you, whatever number of clinics you go on to run. Confirm your email and you are in.",
  },
  {
    title: "Register your clinic",
    body: "Name it and choose its type. A database and a subdomain are created for it there and then.",
  },
  {
    title: "It's live at its subdomain",
    body: "Open yourclinic.jvm.my.id, invite your team, and start adding people and units.",
  },
];
