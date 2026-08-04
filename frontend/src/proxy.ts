import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Makes a tenant's subdomain a working front door.
 *
 * Wildcard DNS and a wildcard certificate get `namaklinik.jvm.my.id` as far as
 * this application, but the pages are addressed by path — `/organizations/
 * {slug}/…` — so without this the subdomain would land on whatever the root
 * happens to render, which is not that clinic. Here the host name is read back
 * into a slug and the request is sent to the canonical URL for it.
 *
 * A redirect rather than a rewrite, which is the whole reason this stays short:
 * the session cookies are httpOnly and scoped to the host that set them, so
 * serving the app from many hosts would mean widening them to `.jvm.my.id` —
 * one cookie shared by every tenant's origin. Sending the browser to one origin
 * keeps the session on one host and leaves that decision unmade.
 *
 * Unset `APP_ORIGIN` and nothing happens, which is the local and preview case:
 * localhost carries no tenant anyway.
 */

const BASE_DOMAIN = (process.env.NEXT_PUBLIC_TENANT_BASE_DOMAIN ?? "jvm.my.id").toLowerCase();

/** Where the application is actually served, e.g. `https://app.jvm.my.id`. */
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
  if (!APP_ORIGIN) return NextResponse.next();

  const host = hostOf(request);
  const slug = tenantOf(host);
  if (!slug) return NextResponse.next();

  const target = new URL(APP_ORIGIN);
  // A misconfiguration that pointed APP_ORIGIN at a tenant host would otherwise
  // redirect to itself for ever.
  if (target.host.toLowerCase() === host) return NextResponse.next();

  const { pathname, search } = request.nextUrl;
  // The subdomain names the organization and nothing else, so only the root
  // means "show me this clinic". Deeper paths are carried across untouched:
  // a link that was shared with the wrong host still arrives where it meant to.
  target.pathname = pathname === "/" ? `/organizations/${slug}` : pathname;
  target.search = search;

  // 307, so a form post that reached the wrong host is not silently turned
  // into a GET on the right one.
  return NextResponse.redirect(target, 307);
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
  // Everything but the assets: a redirect on those would cost a round trip and
  // reach the same file anyway.
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.(?:png|jpg|jpeg|svg|webp|ico|txt|xml)$).*)"],
};
