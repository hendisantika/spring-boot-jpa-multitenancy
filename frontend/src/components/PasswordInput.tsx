"use client";

import { useState, type ComponentProps } from "react";

import { FIELD } from "@/components/ui";

/**
 * A password field that can be read back.
 *
 * Typing a password nobody can see is how a signup gets abandoned and how a
 * "wrong password" turns out to have been a stray capital — and the usual reply
 * to that, a second "confirm" box, only doubles the guessing. So the field
 * carries its own eye.
 *
 * `type` is not accepted: a control that talks about showing and hiding a
 * password would be a lie on anything else, and it is the one prop this owns.
 */
export function PasswordInput({ className = "", ...props }: Omit<ComponentProps<"input">, "type">) {
  const [shown, setShown] = useState(false);

  return (
    // Sits inside the <label> that Field draws, so the input stays associated
    // with its text. The button does not steal that click: a browser only
    // forwards a label's activation when the target is not interactive itself.
    <span className="relative block">
      <input
        {...props}
        type={shown ? "text" : "password"}
        // Room for the button, or a long password runs underneath it.
        className={`${FIELD} pr-11 ${className}`}
      />
      <button
        type="button"
        onClick={() => setShown((was) => !was)}
        // The label says what the click will do rather than what the state is,
        // which is the thing a screen reader user is actually choosing between.
        aria-label={shown ? "Hide password" : "Show password"}
        className="absolute inset-y-0 right-0 grid w-11 place-items-center rounded-r-lg text-ink-muted transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/20"
      >
        {shown ? <EyeOffIcon /> : <EyeIcon />}
      </button>
    </span>
  );
}

function EyeIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="size-5"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" />
      <path d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="size-5"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243" />
    </svg>
  );
}
