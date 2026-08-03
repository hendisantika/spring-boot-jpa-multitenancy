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
  /**
   * An address asked for and not yet confirmed. The account still signs in as
   * `email` until the link sent to this one is opened.
   */
  pendingEmail: string | null;
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
  /** The account's photo, signed and short-lived, or null when there is none. */
  photoUrl: string | null;
};

/** One membership, whole. */
export type MemberDetail = Member & {
  accountId: number;
  phoneNumber: string | null;
  emailVerified: boolean;
  /** Null on memberships older than the column: unknown, not "no time". */
  joinedAt: string | null;
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

export type InvitationState = "PENDING" | "ACCEPTED" | "REVOKED";

export type Invitation = {
  id: number;
  email: string;
  role: TenantRole;
  expiresAt: string;
  status: InvitationState;
  /** PENDING but past its date. Nothing sweeps them, so the status alone lies. */
  expired: boolean;
  /**
   * Whether accepting grants an account that already exists or makes one. No
   * photo goes with it: an invited address may belong to somebody who is not a
   * member of anything here and has agreed to nothing.
   */
  accountExists: boolean;
};

export type CreatedInvitation = Invitation & { emailed: boolean; acceptUrl: string | null };

/**
 * One invitation, whole. No accept link: the token is stored as a hash, so it
 * exists in the recipient's mailbox and nowhere else.
 */
export type InvitationDetail = Invitation & {
  invitedBy: string | null;
  createdAt: string;
  acceptedAt: string | null;
};

export type InvitationPreview = {
  email: string;
  role: TenantRole;
  tenantSlug: string;
  organizationName: string;
  accountExists: boolean;
  expiresAt: string;
};

/** One page of a list the backend refuses to hand over whole. */
export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/**
 * One value from a tenant's own reference lists. `code` is what records store,
 * `label` is only ever shown.
 */
export type ReferenceValue = {
  id: number;
  category: string;
  code: string;
  label: string;
  sortOrder: number;
  active: boolean;
  systemDefined: boolean;
};

/** Every list a tenant keeps, keyed by category. */
export type ReferenceLists = Record<string, ReferenceValue[]>;

/**
 * `MARITAL_STATUS` as "Marital status". Derived rather than looked up in a map:
 * the catalogue is a migration file and a tenant may add categories to it, so a
 * hard-coded list would go stale without anything failing to say so.
 */
export function categoryName(category: string): string {
  const spaced = category.replace(/_/g, " ").toLowerCase();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

/** A person inside a tenant's own database. */
export type TenantPerson = {
  id: number;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  mobile: string | null;
  /** A calendar date, `YYYY-MM-DD`, never an instant — a birthday has no zone. */
  birthDate: string | null;
  gender: string | null;
  maritalStatus: string | null;
  bloodType: string | null;
  identityDocumentType: string | null;
  identityNumber: string | null;
  /** Signed and short-lived, or null when there is none. */
  photoUrl: string | null;
  /** The unit this person belongs to: the id filters, the name is read. */
  unitId: number | null;
  unitName: string | null;
};

/** The label to show for a stored code, falling back to the code itself. */
export function referenceLabel(
  values: ReferenceValue[] | undefined,
  code: string | null,
): string | null {
  if (!code) return null;
  return values?.find((value) => value.code === code)?.label ?? code;
}

/**
 * A business unit inside a tenant's own database. The backend calls these
 * organizations, which collides with the organization that owns the tenant, so
 * the UI does not.
 */
export type TenantUnit = {
  id: number;
  name: string | null;
  address: string | null;
  email: string | null;
  unitType: string | null;
  operatingStatus: string | null;
  province: string | null;
  /** Signed and short-lived, or null when the unit has none. */
  photoUrl: string | null;
};
