/**
 * The search and the page live in the URL rather than in state, which is what
 * lets a link to page 3 of "santoso" still mean that when it is opened again.
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

/** What to ask the API for. */
export function listingQuery(query: string, page: number): string {
  const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  if (query) params.set("q", query);
  return params.toString();
}

/**
 * A link back into the screen, keeping whatever the search and page are. Blank
 * values are left out so the plain screen keeps a plain URL.
 */
export function listingUrl(
  base: string,
  query: string,
  page: number,
  extra: Record<string, string> = {},
): string {
  const params = new URLSearchParams();
  if (query) params.set("q", query);
  if (page > 0) params.set("page", String(page));
  for (const [key, value] of Object.entries(extra)) params.set(key, value);
  const suffix = params.toString();
  return suffix ? `${base}?${suffix}` : base;
}
