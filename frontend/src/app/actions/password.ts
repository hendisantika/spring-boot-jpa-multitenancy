"use server";

import { redirect } from "next/navigation";

import { ApiError, api } from "@/lib/api";
import type { FormState } from "@/lib/types";

/**
 * resetUrl comes back only when mail delivery is off, so the flow stays
 * followable without a mail account.
 */
export type ForgotState = FormState & { message?: string; resetUrl?: string | null };

export async function requestPasswordReset(
  _prev: ForgotState,
  formData: FormData,
): Promise<ForgotState> {
  const email = String(formData.get("email") ?? "").trim();

  try {
    const response = await api<{ message: string; resetUrl: string | null }>(
      "/api/auth/password/forgot",
      { method: "POST", json: { email }, anonymous: true },
    );
    // Deliberately the same answer whether or not the address has an account.
    return { ok: true, message: response.message, resetUrl: response.resetUrl };
  } catch (error) {
    return { error: messageOf(error), values: { email } };
  }
}

export async function resetPassword(_prev: FormState, formData: FormData): Promise<FormState> {
  const token = String(formData.get("token") ?? "");
  const password = String(formData.get("password") ?? "");

  if (password !== String(formData.get("confirmPassword") ?? "")) {
    return { error: "The two passwords do not match." };
  }

  try {
    await api<{ email: string }>(`/api/auth/password/reset/${token}`, {
      method: "POST",
      json: { password },
      anonymous: true,
    });
  } catch (error) {
    return { error: messageOf(error) };
  }

  redirect("/login?reset=1");
}

function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message.includes("fetch failed")) {
    return "Cannot reach the API. Is the backend running on port 8080?";
  }
  return "Something went wrong. Please try again.";
}
