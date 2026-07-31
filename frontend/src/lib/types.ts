export type TenantRole = "OWNER" | "MEMBER";

export const ORG_STRUCTURES = [
  { value: "SINGLE_LOCATION_CLINIC", label: "Single Location Clinic" },
  { value: "MULTI_LOCATION_CLINIC", label: "Multi Location Clinic" },
  { value: "SINGLE_LOCATION_HOSPITAL", label: "Single Location Hospital" },
  { value: "MULTI_LOCATION_HOSPITAL", label: "Multi Location Hospital" },
] as const;

export const PRACTICE_SPECIALITIES = [
  { value: "GENERAL_PRACTICE", label: "General Practice" },
  { value: "SPECIALIST_PRACTICE", label: "Specialist Practice" },
  { value: "MULTIPLE_PRACTICES_MEDICAL_GROUP", label: "Multiple Practices / Medical Group" },
  { value: "HOSPITAL", label: "Hospital" },
  { value: "DENTAL", label: "Dental" },
  { value: "AESTHETIC_AND_DERMA", label: "Aesthetic & Derma" },
  { value: "ALLIED_HEALTH", label: "Allied Health" },
  { value: "MENTAL_HEALTH", label: "Mental Health" },
  { value: "OTHERS", label: "Others" },
] as const;

export type Account = {
  id: number;
  email: string;
  phoneNumber: string | null;
  photoUrl: string | null;
  status: string;
  emailVerified: boolean;
};

export type TokenPair = {
  accessToken: string;
  refreshToken: string;
  memberships: Record<string, TenantRole>;
};

export type Organization = {
  slug: string;
  businessName: string;
  businessEmail: string | null;
  photoUrl: string | null;
  contactFirstName: string | null;
  contactLastName: string | null;
  jobTitle: string | null;
  phoneNumber: string | null;
  orgStructure: string | null;
  practiceSpeciality: string | null;
  databaseName: string;
  subdomain: string;
  status: string;
};

export type Member = {
  accountId: number | null;
  email: string;
  role: TenantRole;
};

/**
 * What a server action hands back to a form. `values` carries the submitted
 * fields back so a rejected form re-renders filled in rather than blank.
 * Passwords are deliberately never included.
 */
export type FormState = {
  error?: string;
  ok?: boolean;
  values?: Record<string, string>;
};

export function labelOf(
  options: ReadonlyArray<{ value: string; label: string }>,
  value: string | null,
): string {
  if (!value) return "—";
  return options.find((option) => option.value === value)?.label ?? value;
}

export type Invitation = {
  id: number;
  email: string;
  role: TenantRole;
  expiresAt: string;
};

export type CreatedInvitation = Invitation & { emailed: boolean; acceptUrl: string | null };

export type InvitationPreview = {
  email: string;
  role: TenantRole;
  tenantSlug: string;
  organizationName: string;
  accountExists: boolean;
  expiresAt: string;
};
