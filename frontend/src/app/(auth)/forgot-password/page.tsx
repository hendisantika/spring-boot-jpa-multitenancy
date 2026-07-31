import Link from "next/link";

import { ForgotPasswordForm } from "./ForgotPasswordForm";
import { Card } from "@/components/ui";

export const metadata = { title: "Forgot your password" };

export default function ForgotPasswordPage() {
  return (
    <Card className="p-6">
      <h1 className="text-xl font-semibold tracking-tight text-ink">Forgot your password</h1>
      <p className="mt-1 mb-6 text-sm text-ink-muted">
        Enter your email and we will send a link to choose a new one.
      </p>

      <ForgotPasswordForm />

      <p className="mt-6 text-center text-sm text-ink-muted">
        <Link href="/login" className="font-medium text-brand hover:underline">
          Back to sign in
        </Link>
      </p>
    </Card>
  );
}
