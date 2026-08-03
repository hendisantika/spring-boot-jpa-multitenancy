import { AccountEmailForm } from "./AccountEmailForm";
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
        </Card>

        <Card className="p-6">
          <h2 className="mb-4 font-semibold text-ink">Photo</h2>
          {account ? (
            <AccountPhotoForm photoUrl={account.photoUrl} />
          ) : (
            <Alert>Cannot reach the API, so the photo cannot be changed right now.</Alert>
          )}
        </Card>

        <Card className="p-6">
          <h2 className="font-semibold text-ink">Email</h2>
          <p className="mt-1 mb-4 text-sm text-ink-muted">
            {/* Set out before the form rather than discovered after submitting. */}
            The new address has to be confirmed from a link sent to it, and nothing changes until
            then — so a mistyped address costs an email rather than the account.
          </p>
          {account ? (
            <AccountEmailForm email={account.email} pendingEmail={account.pendingEmail} />
          ) : (
            <Alert>Cannot reach the API, so the email cannot be changed right now.</Alert>
          )}
        </Card>
      </div>
    </>
  );
}
