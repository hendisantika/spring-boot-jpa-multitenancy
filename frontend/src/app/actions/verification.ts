"use server";

import { revalidatePath } from "next/cache";

import { ApiError, api } from "@/lib/api";
import type { FormState } from "@/lib/types";

export type ResendState = FormState & { message?: string; verifyUrl?: string | null };

export async function resendVerification(): Promise<ResendState> {
  try {
    const response = await api<{ message: string; resetUrl: string | null }>(
      "/api/auth/verify-email/resend",
      { method: "POST" },
    );
    revalidatePath("/dashboard");
    return { ok: true, message: response.message, verifyUrl: response.resetUrl };
  } catch (error) {
    return { error: error instanceof ApiError ? error.message : "Could not send the link." };
  }
}
