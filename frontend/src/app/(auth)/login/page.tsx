import Link from "next/link";

import { LoginForm } from "./LoginForm";
import { Card } from "@/components/ui";

export const metadata = { title: "Sign in" };

export default function LoginPage() {
  return (
    <Card className="p-6">
      <h1 className="text-xl font-semibold tracking-tight text-ink">Sign in</h1>
      <p className="mt-1 mb-6 text-sm text-ink-muted">
        The parent login. Your organizations come from the account, not the address.
      </p>

      <LoginForm />

      <p className="mt-6 text-center text-sm text-ink-muted">
        No account yet?{" "}
        <Link href="/signup" className="font-medium text-brand hover:underline">
          Create one
        </Link>
      </p>
    </Card>
  );
}
