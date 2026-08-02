"use client";

import { useActionState } from "react";

import { updateOrganization } from "@/app/actions/organizations";
import { PhotoField } from "@/components/PhotoField";
import { SubmitButton } from "@/components/SubmitButton";
import { Alert, Field, Input, Select } from "@/components/ui";
import {
  ORG_STRUCTURES,
  PRACTICE_SPECIALITIES,
  type FormState,
  type Organization,
} from "@/lib/types";

export function EditOrganizationForm({ organization }: { organization: Organization }) {
  const [state, action] = useActionState<FormState, FormData>(updateOrganization, {});
  // What was submitted wins over what was loaded, so a rejected form keeps edits.
  const value = (field: keyof Organization) =>
    state.values?.[field] ?? (organization[field] as string | null) ?? "";

  return (
    <form action={action} className="space-y-5">
      <input type="hidden" name="slug" value={organization.slug} />

      {state.error ? <Alert>{state.error}</Alert> : null}

      <Field
        label="Business name"
        hint={`The address stays ${organization.subdomain} and the database stays \`${organization.databaseName}\`; renaming changes the label only.`}
      >
        <Input name="businessName" required maxLength={100} defaultValue={value("businessName")} />
      </Field>

      <Field label="Business email">
        <Input name="businessEmail" type="email" required defaultValue={value("businessEmail")} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Contact first name">
          <Input name="contactFirstName" required maxLength={100} defaultValue={value("contactFirstName")} />
        </Field>
        <Field label="Contact last name">
          <Input name="contactLastName" required maxLength={100} defaultValue={value("contactLastName")} />
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Job title">
          <Input name="jobTitle" required maxLength={100} defaultValue={value("jobTitle")} />
        </Field>
        <Field label="Phone number">
          <Input
            name="phoneNumber"
            type="tel"
            required
            pattern="^\+?[0-9 ()-]{6,30}$"
            defaultValue={value("phoneNumber")}
          />
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Organization structure">
          <Select name="orgStructure" required defaultValue={value("orgStructure")}>
            {ORG_STRUCTURES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Practice speciality">
          <Select name="practiceSpeciality" required defaultValue={value("practiceSpeciality")}>
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
        hint="Leave empty to keep the current one. JPEG, PNG or WebP, up to 5 MB."
        currentUrl={organization.photoUrl}
        round={false}
      />

      <div className="flex justify-end gap-3 border-t border-line pt-5">
        <SubmitButton pendingLabel="Saving…">Save changes</SubmitButton>
      </div>
    </form>
  );
}
