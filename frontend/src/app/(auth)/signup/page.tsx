import Link from "next/link";

import { SignupForm } from "./SignupForm";
import { Card } from "@/components/ui";

export const metadata = { title: "Create your account" };

export default function SignupPage() {
  return (
    <Card className="p-6">
      <h1 className="text-xl font-semibold tracking-tight text-ink">Create your account</h1>
      <p className="mt-1 mb-6 text-sm text-ink-muted">
        You will own the organization you register next.
      </p>

      <SignupForm />

      <p className="mt-6 text-center text-sm text-ink-muted">
        Already have an account?{" "}
        <Link href="/login" className="font-medium text-brand hover:underline">
          Sign in
        </Link>
      </p>
    </Card>
  );
}
