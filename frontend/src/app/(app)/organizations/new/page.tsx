import Link from "next/link";

import { OrganizationForm } from "./OrganizationForm";
import { Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Register organization" };

export default function NewOrganizationPage() {
  return (
    <>
      <Link href="/dashboard" className="mb-4 inline-block text-sm text-ink-muted hover:text-ink">
        ← Back
      </Link>
      <PageHeading
        title="Register organization"
        description="Submitting this creates a MySQL database and a subdomain for it, and makes you its owner."
      />
      <Card className="p-6">
        <OrganizationForm />
      </Card>
    </>
  );
}
