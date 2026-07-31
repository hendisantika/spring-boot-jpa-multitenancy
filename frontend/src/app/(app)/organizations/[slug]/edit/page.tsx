import Link from "next/link";

import { EditOrganizationForm } from "./EditOrganizationForm";
import { ApiError, api } from "@/lib/api";
import { getRole } from "@/lib/session";
import type { Organization } from "@/lib/types";
import { Alert, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Edit organization" };

export default async function EditOrganizationPage({ params }: PageProps<"/organizations/[slug]/edit">) {
  const { slug } = await params;
  const role = await getRole(slug);

  // Shown rather than fetched: the API refuses a member anyway, so there is no
  // point loading the form only to have saving fail.
  if (role !== "OWNER") {
    return (
      <>
        <PageHeading title="Edit organization" />
        <Alert>Only the owner of this organization can edit it.</Alert>
      </>
    );
  }

  let organization: Organization | null = null;
  let error: string | null = null;

  try {
    organization = await api<Organization>(`/api/organizations/${slug}`);
  } catch (e) {
    error = e instanceof ApiError ? e.message : "Cannot reach the API.";
  }

  return (
    <>
      <Link href={`/organizations/${slug}`} className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
        ← Back
      </Link>
      <PageHeading title="Edit organization" description="The database and subdomain stay as they are." />

      {error || !organization ? (
        <Alert>{error ?? "Not found."}</Alert>
      ) : (
        <Card className="p-6">
          <EditOrganizationForm organization={organization} />
        </Card>
      )}
    </>
  );
}
