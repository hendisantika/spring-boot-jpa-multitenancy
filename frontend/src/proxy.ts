import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Serves a tenant's own subdomain in place, so opening `namaklinik.jvm.my.id`
 * shows that clinic and stays there.
 *
 * The pages are addressed by path — `/organizations/{slug}/…` — so the bare
 * root of a tenant host is rewritten to that clinic's landing page. The browser
 * keeps the address it asked for; only the path the app renders is changed.
 *
 * A rewrite, not a redirect: the host stays `namaklinik.jvm.my.id` rather than
 * bouncing to one shared origin. The session that follows is therefore scoped
 * to that host — each clinic is its own login. The central host (APP_ORIGIN,
 * e.g. dev.jvm.my.id) is where accounts are made and organizations registered;
 * it is deliberately excluded here so it is never mistaken for a tenant named
 * "dev".
 *
 * Deeper paths are left untouched: `/login`, `/account`, and the clinic's own
 * `/organizations/{slug}/…` pages are already real routes, served as they are
 * on whichever host asked for them.
 *
 * Unset `APP_ORIGIN` and the central-host guard is skipped, which is the local
 * and preview case: localhost carries no tenant anyway.
 */

const BASE_DOMAIN = (process.env.NEXT_PUBLIC_TENANT_BASE_DOMAIN ?? "jvm.my.id").toLowerCase();

/** The central host where accounts and registration live, e.g. dev.jvm.my.id. */
const APP_ORIGIN = process.env.APP_ORIGIN?.trim();

/**
 * Labels the backend refuses to hand out, mirroring
 * `application.tenant.reserved-slugs`. They are hosts in their own right, so a
 * request to one is not a request for a tenant.
 */
const RESERVED = new Set([
  "www", "api", "admin", "app", "mail", "ftp", "test", "staging", "default",
]);

/** The backend's rule, so a host that could never be a tenant is not treated as one. */
const VALID_SLUG = /^[a-z][a-z0-9]{2,29}$/;

export function proxy(request: NextRequest) {
  const host = hostOf(request);
  const slug = tenantOf(host);
  if (!slug) return NextResponse.next();

  // The central host reads as a valid slug ("dev") but is not a tenant. Skip it
  // so its own pages are served rather than rewritten to /organizations/dev.
  if (APP_ORIGIN && host === new URL(APP_ORIGIN).host.toLowerCase()) {
    return NextResponse.next();
  }

  // Only the bare root means "show me this clinic". Everything else is already a
  // real route and is served in place, so the session set on this host stays on
  // this host.
  if (request.nextUrl.pathname === "/") {
    const url = request.nextUrl.clone();
    url.pathname = `/organizations/${slug}`;
    return NextResponse.rewrite(url);
  }
  return NextResponse.next();
}

/** The requested host, lowercased and without its port. */
function hostOf(request: NextRequest): string {
  const header = request.headers.get("host") ?? "";
  return header.toLowerCase().replace(/:\d+$/, "");
}

/**
 * @return the tenant slug, or null when the host is the apex, a reserved name,
 * something outside the base domain, or deeper than one label — the same
 * reading `TenantSubdomainInterceptor` performs on the backend.
 */
function tenantOf(host: string): string | null {
  if (!host.endsWith(`.${BASE_DOMAIN}`)) return null;
  const label = host.slice(0, -(BASE_DOMAIN.length + 1));
  if (label.includes(".")) return null;
  if (RESERVED.has(label)) return null;
  return VALID_SLUG.test(label) ? label : null;
}

export const config = {
  // Everything but the assets: rewriting those would reach the same file anyway.
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.(?:png|jpg|jpeg|svg|webp|ico|txt|xml)$).*)"],
};
