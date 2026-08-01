/**
 * The search, the filters and the page live in the URL rather than in state,
 * which is what lets a link to page 3 of "santoso, blood type O+" still mean
 * that when it is opened again.
 *
 * Both screens that list a tenant's data read and write those parameters the
 * same way, so the rules are here rather than copied.
 */

/** How many rows a screen asks for. The backend clamps its own maximum. */
export const PAGE_SIZE = 10;

/** A repeated `?q=a&q=b` is somebody poking at the URL, so take the first. */
export function firstValue(value: string | string[] | undefined): string {
  return typeof value === "string" ? value : "";
}

/**
 * Every value a parameter was given. Filters are held this way whether or not
 * they accept several, so one set of URL rules serves both rather than two that
 * could drift apart. A single-valued filter simply never has more than one.
 */
function allValues(value: string | string[] | undefined): string[] {
  const values = typeof value === "string" ? [value] : (value ?? []);
  return values.map((one) => one.trim()).filter(Boolean);
}

/** What one screen is currently showing. */
export type Listing = {
  /** The route the screen lives at, which is also where its form submits. */
  base: string;
  query: string;
  page: number;
  /** Chosen filters by field name. Blank ones are never kept. */
  filters: Record<string, string[]>;
};

type SearchParams = Record<string, string | string[] | undefined>;

/**
 * Reads the listing out of the URL. Only the named filters are picked up, so an
 * unrelated parameter cannot be smuggled through into an API call.
 */
export function readListing(base: string, searchParams: SearchParams, filterNames: string[]): Listing {
  const filters: Record<string, string[]> = {};
  for (const name of filterNames) {
    const values = allValues(searchParams[name]);
    if (values.length) filters[name] = values;
  }
  return {
    base,
    query: firstValue(searchParams.q).trim(),
    page: Math.max(0, Number(firstValue(searchParams.page)) || 0),
    filters,
  };
}

function appendFilters(params: URLSearchParams, listing: Listing): void {
  for (const [name, values] of Object.entries(listing.filters)) {
    for (const value of values) params.append(name, value);
  }
}

/** What to ask the API for. */
export function apiQuery(listing: Listing): string {
  const params = new URLSearchParams({ page: String(listing.page), size: String(PAGE_SIZE) });
  if (listing.query) params.set("q", listing.query);
  appendFilters(params, listing);
  return params.toString();
}

/**
 * A link back into the screen, keeping whatever the search, filters and page
 * are. Blank values are left out so a plain screen keeps a plain URL.
 */
export function listingUrl(
  listing: Listing,
  overrides: { page?: number; extra?: Record<string, string> } = {},
): string {
  const page = overrides.page ?? listing.page;
  const params = new URLSearchParams();
  if (listing.query) params.set("q", listing.query);
  appendFilters(params, listing);
  if (page > 0) params.set("page", String(page));
  for (const [key, value] of Object.entries(overrides.extra ?? {})) params.set(key, value);
  const suffix = params.toString();
  return suffix ? `${listing.base}?${suffix}` : listing.base;
}

/** Whether anything is narrowing the list, which is what a Clear link is for. */
export function isNarrowed(listing: Listing): boolean {
  return Boolean(listing.query) || Object.keys(listing.filters).length > 0;
}

/** What a single-valued filter currently holds, for a dropdown to preselect. */
export function chosen(listing: Listing, name: string): string {
  return listing.filters[name]?.[0] ?? "";
}

/** What a multi-valued filter currently holds. */
export function chosenAll(listing: Listing, name: string): string[] {
  return listing.filters[name] ?? [];
}
