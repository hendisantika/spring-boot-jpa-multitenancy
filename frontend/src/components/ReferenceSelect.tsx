import { Field, Select } from "@/components/ui";
import type { ReferenceLists } from "@/lib/types";

/**
 * A dropdown filled from one of the tenant's own reference lists.
 *
 * A blank option comes first, because these fields are optional and "not
 * recorded" is a real answer.
 *
 * Only values still on offer are listed — but a record written before one was
 * retired still holds that code, so it is kept as its own option, by its real
 * label, and marked. Dropping it would silently rewrite the record the moment
 * anybody opened the form.
 *
 * None of this is a check. The API refuses a code that is not in its list
 * whatever this page offers.
 */
export function ReferenceSelect({
  label,
  name,
  category,
  lists,
  current,
  blank = "—",
  hint = "Optional",
}: {
  label: string;
  /** The form field, which is also the property on the record. */
  name: string;
  /** Which list in `lists` to draw from, such as `BLOOD_TYPE`. */
  category: string;
  lists: ReferenceLists;
  current: string;
  /** What the empty option reads as: "—" on a form, "Any" on a filter. */
  blank?: string;
  hint?: string;
}) {
  const values = lists[category] ?? [];
  const offered = values.filter((value) => value.active);
  const isRetired = Boolean(current) && !offered.some((value) => value.code === current);
  const retiredLabel = values.find((value) => value.code === current)?.label ?? current;

  return (
    <Field label={label} hint={hint}>
      <Select name={name} defaultValue={current}>
        <option value="">{blank}</option>
        {offered.map((value) => (
          <option key={value.code} value={value.code}>
            {value.label}
          </option>
        ))}
        {isRetired ? <option value={current}>{retiredLabel} (no longer offered)</option> : null}
      </Select>
    </Field>
  );
}
