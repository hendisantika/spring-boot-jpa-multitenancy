"use client";

import { useActionState, useState } from "react";

import { registerOrganization } from "@/app/actions/organizations";
import { PhotoField } from "@/components/PhotoField";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input, Select } from "@/components/ui";
import { ORG_STRUCTURES, PRACTICE_SPECIALITIES, type FormState } from "@/lib/types";

/** Mirrors TenantSlugs on the backend, so the preview matches what is created. */
function slugify(name: string): string {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, "");
}

const BASE_DOMAIN = process.env.NEXT_PUBLIC_TENANT_BASE_DOMAIN ?? "jvm.my.id";

export function OrganizationForm() {
  const [state, action] = useActionState<FormState, FormData>(registerOrganization, {});
  const submitted = state.values ?? {};
  const [businessName, setBusinessName] = useState(submitted.businessName ?? "");


  const slug = slugify(businessName);
  const slugIsUsable = slug.length >= 3 && slug.length <= 30 && /^[a-z]/.test(slug);

  return (
    <form action={action} className="space-y-5">
      {state.error ? <Alert>{state.error}</Alert> : null}

      <Field
        label="Business name"
        hint={
          businessName
            ? slugIsUsable
              ? `Database \`${slug}\` · ${slug}.${BASE_DOMAIN}`
              : "Needs 3 to 30 letters or digits and must start with a letter."
            : "The database name and the subdomain are derived from this."
        }
      >
        <Input
          name="businessName"
          required
          maxLength={100}
          value={businessName}
          onChange={(event) => setBusinessName(event.target.value)}
          placeholder="Klinik Sehat"
        />
      </Field>

      <Field label="Business email">
        <Input
          name="businessEmail"
          type="email"
          required
          placeholder="clinic@example.com"
          defaultValue={submitted.businessEmail ?? ""}
        />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Contact first name">
          <Input name="contactFirstName" required maxLength={100} defaultValue={submitted.contactFirstName ?? ""} />
        </Field>
        <Field label="Contact last name">
          <Input name="contactLastName" required maxLength={100} defaultValue={submitted.contactLastName ?? ""} />
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Job title">
          <Input
            name="jobTitle"
            required
            maxLength={100}
            placeholder="Practice Manager"
            defaultValue={submitted.jobTitle ?? ""}
          />
        </Field>
        <Field label="Phone number">
          <Input
            name="phoneNumber"
            type="tel"
            required
            placeholder="+62 812 3456 7890"
            pattern="^\+?[0-9 ()-]{6,30}$"
            defaultValue={submitted.phoneNumber ?? ""}
          />
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Organization structure">
          <Select name="orgStructure" required defaultValue={submitted.orgStructure ?? ""}>
            <option value="" disabled>
              Choose one
            </option>
            {ORG_STRUCTURES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Practice speciality">
          <Select name="practiceSpeciality" required defaultValue={submitted.practiceSpeciality ?? ""}>
            <option value="" disabled>
              Choose one
            </option>
            {PRACTICE_SPECIALITIES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </Field>
      </div>

      <PhotoField
        label="Organization photo"
        hint="Optional. JPEG, PNG or WebP, up to 5 MB."
        round={false}
      />

      <div className="flex justify-end gap-3 border-t border-line pt-5">
        <SubmitButton pendingLabel="Creating database…">Register organization</SubmitButton>
      </div>
    </form>
  );
}
