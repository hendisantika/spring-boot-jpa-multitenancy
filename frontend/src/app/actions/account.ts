"use server";

import { revalidatePath } from "next/cache";

import { ApiError, api } from "@/lib/api";
import type { Account, FormState } from "@/lib/types";

/**
 * The photo on your own account. Only the photo: the email is what you sign in
 * with, and changing it means verifying the new one first.
 */
export async function saveAccountPhoto(_prev: FormState, formData: FormData): Promise<FormState> {
  const photo = formData.get("photo");
  const removing = formData.get("removePhoto") === "true";

  if (!(photo instanceof File && photo.size > 0) && !removing) {
    return { error: "Choose a photo, or tick the box to remove the current one." };
  }

  const payload = new FormData();
  if (photo instanceof File && photo.size > 0) {
    payload.append("photo", photo, photo.name);
  } else {
    payload.append("removePhoto", "true");
  }

  try {
    await api<Account>("/api/auth/me/photo", { method: "PUT", body: payload });
  } catch (error) {
    if (error instanceof ApiError) return { error: error.message };
    if (error instanceof Error && error.message.includes("fetch failed")) {
      return { error: "Cannot reach the API." };
    }
    return { error: "Something went wrong. Please try again." };
  }

  // The header on every page shows this, so the whole tree is stale, not just
  // this route.
  revalidatePath("/", "layout");
  return { ok: true };
}
