import { getAccessToken } from "@/lib/session";

/**
 * Server side only: the browser never talks to the backend directly, so the
 * token stays in an httpOnly cookie.
 */
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

type ApiOptions = {
  method?: string;
  body?: BodyInit;
  json?: unknown;
  /** Sent as X-Tenant, which the backend accepts in place of a subdomain. */
  tenant?: string;
  anonymous?: boolean;
};

export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const headers = new Headers();

  if (!options.anonymous) {
    const token = await getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }
  if (options.tenant) headers.set("X-Tenant", options.tenant);

  let body = options.body;
  if (options.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.json);
  }

  const response = await fetch(`${BACKEND_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body,
    cache: "no-store",
  });

  if (!response.ok) {
    throw new ApiError(await messageFrom(response), response.status);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

/**
 * The backend answers failures with RFC 9457 problem details, so prefer the
 * "detail" field and fall back to the field errors a validation failure carries.
 */
async function messageFrom(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as {
      detail?: string;
      errors?: Array<{ field?: string; defaultMessage?: string }>;
      title?: string;
    };
    if (problem.errors?.length) {
      return problem.errors
        .map((e) => [e.field, e.defaultMessage].filter(Boolean).join(" "))
        .join(", ");
    }
    return problem.detail ?? problem.title ?? response.statusText;
  } catch {
    if (response.status === 401) return "Please sign in again.";
    if (response.status === 403) return "You are not allowed to do that.";
    return response.statusText || `Request failed with ${response.status}`;
  }
}

/** A JSON part of a multipart request, which the backend expects for forms. */
export function jsonPart(value: unknown): Blob {
  return new Blob([JSON.stringify(value)], { type: "application/json" });
}

export function backendUrl(): string {
  return BACKEND_URL;
}
