"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

import { ApiError, api, jsonPart } from "@/lib/api";
import type { FormState } from "@/lib/types";

/**
 * Every call carries the tenant, because this data lives in that tenant's own
 * database rather than the central one.
 */
function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) return "Only the owner of this organization can do that.";
    return error.message;
  }
  if (error instanceof Error && error.message.includes("fetch failed")) {
    return "Cannot reach the API. Is the backend running on port 8080?";
  }
  return "Something went wrong. Please try again.";
}

/**
 * Where to land after saving. It arrives as a form field, so it is checked
 * against the screen it claims to belong to rather than trusted: a redirect
 * target a page can choose is a redirect an attacker can choose.
 */
function backTo(formData: FormData, fallback: string): string {
  const target = String(formData.get("backTo") ?? "");
  return target === fallback || target.startsWith(`${fallback}?`) ? target : fallback;
}

function personFrom(formData: FormData) {
  const field = (name: string) => String(formData.get(name) ?? "").trim() || null;
  return {
    firstName: String(formData.get("firstName") ?? "").trim(),
    lastName: String(formData.get("lastName") ?? "").trim(),
    email: field("email"),
    mobile: field("mobile"),
    birthDate: field("birthDate"),
    // Codes from the tenant's reference lists. The API checks them again.
    gender: field("gender"),
    maritalStatus: field("maritalStatus"),
    bloodType: field("bloodType"),
    identityDocumentType: field("identityDocumentType"),
    identityNumber: field("identityNumber"),
  };
}

export async function savePerson(_prev: FormState, formData: FormData): Promise<FormState> {
  const slug = String(formData.get("slug") ?? "");
  const id = String(formData.get("id") ?? "");
  const values = personFrom(formData);
  const photo = formData.get("photo");

  // Multipart so the photo arrives with the record rather than in a second
  // call: there is then no window where the person exists without it, and
  // nothing to unpick when only one of the two succeeds. Omitting the part on
  // an edit keeps the current photo, which is what the API expects.
  const payload = new FormData();
  payload.append("person", jsonPart(values), "person.json");
  if (photo instanceof File && photo.size > 0) {
    payload.append("photo", photo, photo.name);
  }

  try {
    if (id) {
      await api(`/person/${id}`, { method: "PUT", body: payload, tenant: slug });
    } else {
      await api("/person", { method: "POST", body: payload, tenant: slug });
    }
  } catch (error) {
    return { error: messageOf(error), values: Object.fromEntries(
      Object.entries(values).map(([k, v]) => [k, v ?? ""])) };
  }

  revalidatePath(`/organizations/${slug}/people`);
  redirect(backTo(formData, `/organizations/${slug}/people`));
}

export async function deletePerson(formData: FormData) {
  const slug = String(formData.get("slug") ?? "");
  try {
    await api(`/person/${formData.get("id")}`, { method: "DELETE", tenant: slug });
  } catch {
    // The page re-renders from the server, so a refusal simply shows no change.
  }
  revalidatePath(`/organizations/${slug}/people`);
}

function unitFrom(formData: FormData) {
  const field = (name: string) => String(formData.get(name) ?? "").trim() || null;
  return {
    name: String(formData.get("name") ?? "").trim(),
    address: field("address"),
    email: field("email"),
    // Codes from the tenant's reference lists. The API checks them again.
    unitType: field("unitType"),
    operatingStatus: field("operatingStatus"),
    province: field("province"),
  };
}

export async function saveUnit(_prev: FormState, formData: FormData): Promise<FormState> {
  const slug = String(formData.get("slug") ?? "");
  const id = String(formData.get("id") ?? "");
  const values = unitFrom(formData);

  try {
    if (id) {
      await api(`/organization/${id}`, { method: "PUT", json: values, tenant: slug });
    } else {
      await api("/organization", { method: "POST", json: values, tenant: slug });
    }
  } catch (error) {
    return { error: messageOf(error), values: Object.fromEntries(
      Object.entries(values).map(([k, v]) => [k, v ?? ""])) };
  }

  revalidatePath(`/organizations/${slug}/units`);
  redirect(backTo(formData, `/organizations/${slug}/units`));
}

export async function deleteUnit(formData: FormData) {
  const slug = String(formData.get("slug") ?? "");
  try {
    await api(`/organization/${formData.get("id")}`, { method: "DELETE", tenant: slug });
  } catch {
    // Same: the refusal shows as nothing changing.
  }
  revalidatePath(`/organizations/${slug}/units`);
}
