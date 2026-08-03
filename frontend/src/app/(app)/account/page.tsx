import { AccountPhotoForm } from "./AccountPhotoForm";
import { currentAccount } from "@/lib/account";
import { getEmail } from "@/lib/session";
import { Alert, Card, PageHeading } from "@/components/ui";

export const metadata = { title: "Your account" };

export default async function AccountPage() {
  const [account, email] = await Promise.all([currentAccount(), getEmail()]);

  return (
    <>
      <PageHeading
        title="Your account"
        description="One account reaches every organization you belong to."
      />

      <div className="mt-6 grid gap-6 lg:grid-cols-[1.2fr_1fr]">
        <Card className="p-6">
          <h2 className="mb-4 font-semibold text-ink">Details</h2>
          <dl className="divide-y divide-line text-sm">
            <div className="flex items-center justify-between py-2">
              <dt className="text-ink-muted">Email</dt>
              <dd className="text-ink">{account?.email ?? email ?? "—"}</dd>
            </div>
            <div className="flex items-center justify-between py-2">
              <dt className="text-ink-muted">Phone</dt>
              <dd className="text-ink">{account?.phoneNumber ?? "—"}</dd>
            </div>
            <div className="flex items-center justify-between py-2">
              <dt className="text-ink-muted">Email confirmed</dt>
              <dd className="text-ink">{account?.emailVerified ? "Yes" : "Not yet"}</dd>
            </div>
          </dl>
          {/* Said plainly rather than left to be discovered by trying. */}
          <p className="mt-4 text-xs text-ink-muted">
            Only the photo can be changed here. The email is what you sign in with, so changing it
            would mean confirming the new one first.
          </p>
        </Card>

        <Card className="p-6">
          <h2 className="mb-4 font-semibold text-ink">Photo</h2>
          {account ? (
            <AccountPhotoForm photoUrl={account.photoUrl} />
          ) : (
            <Alert>Cannot reach the API, so the photo cannot be changed right now.</Alert>
          )}
        </Card>
      </div>
    </>
  );
}
