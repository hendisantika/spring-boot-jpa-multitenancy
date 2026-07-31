"use server";

import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";

import { ApiError, api, jsonPart } from "@/lib/api";
import type { FormState, Member, Organization } from "@/lib/types";

export async function registerOrganization(
  _prev: FormState,
  formData: FormData,
): Promise<FormState> {
  const photo = formData.get("photo");
  const payload = new FormData();

  // Kept so a rejected form comes back filled in; this one has eight fields and
  // retyping them would be miserable.
  const values = {
    businessName: String(formData.get("businessName") ?? "").trim(),
    businessEmail: String(formData.get("businessEmail") ?? "").trim(),
    contactFirstName: String(formData.get("contactFirstName") ?? "").trim(),
    contactLastName: String(formData.get("contactLastName") ?? "").trim(),
    jobTitle: String(formData.get("jobTitle") ?? "").trim(),
    phoneNumber: String(formData.get("phoneNumber") ?? "").trim(),
    orgStructure: String(formData.get("orgStructure") ?? ""),
    practiceSpeciality: String(formData.get("practiceSpeciality") ?? ""),
  };

  payload.append("organization", jsonPart(values), "organization.json");
  if (photo instanceof File && photo.size > 0) {
    payload.append("photo", photo, photo.name);
  }

  let created: Organization;
  try {
    created = await api<Organization>("/api/organizations", { method: "POST", body: payload });
  } catch (error) {
    return { error: messageOf(error), values };
  }

  revalidatePath("/dashboard");
  // The membership just created is not in the current token yet; the detail page
  // explains that and offers the refresh.
  redirect(`/organizations/${created.slug}?fresh=1`);
}

export async function addMember(_prev: FormState, formData: FormData): Promise<FormState> {
  const slug = String(formData.get("slug") ?? "");

  try {
    await api<Member>(`/api/organizations/${slug}/users`, {
      method: "POST",
      json: {
        email: String(formData.get("email") ?? "").trim(),
        phoneNumber: String(formData.get("phoneNumber") ?? "").trim() || null,
        password: String(formData.get("password") ?? ""),
        role: String(formData.get("role") ?? "MEMBER"),
      },
    });
  } catch (error) {
    return { error: messageOf(error) };
  }

  revalidatePath(`/organizations/${slug}`);
  return { ok: true };
}

export async function removeMember(formData: FormData) {
  const slug = String(formData.get("slug") ?? "");
  const accountId = String(formData.get("accountId") ?? "");

  try {
    await api<void>(`/api/organizations/${slug}/users/${accountId}`, { method: "DELETE" });
  } catch {
    // The page re-renders from the server, so a failure simply shows no change.
  }
  revalidatePath(`/organizations/${slug}`);
}

function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message.includes("fetch failed")) {
    return "Cannot reach the API. Is the backend running on port 8080?";
  }
  return "Something went wrong. Please try again.";
}
