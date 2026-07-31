# Frontend

[![Frontend CI](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/frontend.yml/badge.svg)](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/frontend.yml)

The UI for the multi tenancy flow: sign up, register an organization, get a database and a subdomain for it, then add
people to it. Next.js 16 (App Router) with Tailwind CSS 4, running on Bun.

## Running it

The API has to be up first — see the [root README](../README.md).

```bash
cd frontend
cp .env.example .env.local
bun install
bun dev            # http://localhost:3000
```

```bash
bun run build && bun start   # production build
bun run lint
```

## The flow

| Route                   | What happens                                                             |
|-------------------------|--------------------------------------------------------------------------|
| `/signup`               | Email, phone, password and an optional photo. Signs you in straight after |
| `/login`                | The parent login: one account, every organization it belongs to           |
| `/dashboard`            | Only the organizations you are a member of                                |
| `/organizations/new`    | The registration form; creates the database and the subdomain             |
| `/organizations/[slug]` | Profile, the people in it, and inviting people when you are the `OWNER`   |
| `/organizations/[slug]/edit` | Edit the profile; owner only                                        |
| `/invitations/[token]`  | Open: accept an invitation, choosing your own password                   |
| `/forgot-password`      | Ask for a reset link                                                     |
| `/reset-password/[token]` | Open: choose a new password                                            |
| `/verify-email/[token]` | Open: confirm an email address                                           |

## How it is put together

**Tokens never reach the browser.** Login stores the access and refresh tokens in httpOnly cookies, and every call to
the API is made from a server action or a server component. A script on the page cannot read them, and the API base URL
stays server side.

**Forms are server actions** with `useActionState`, so validation errors come back from the same round trip. A rejected
form re-renders with what you typed still in it — everything except the passwords, which are deliberately not sent back.

**Invitations, not shared passwords.** The owner names an email; the recipient opens the link and chooses their own
password, so nobody else ever handles it. The link is shown once, with a copy button, because the backend keeps only a
hash of the token.

**The token lag is explained, not hidden.** An access token carries the tenants the account may reach, so the
organization you just registered is not in the token that registered it. Rather than showing a bare `403`, the page says
so and offers a **Refresh session** button, which exchanges the refresh token for a new pair — and memberships are read
from the database at that moment.

**Roles come from the token.** The "add someone" form appears only for an `OWNER`, and `Remove` is not offered for the
owner, because the backend refuses that anyway.

**The subdomain preview mirrors the backend.** Typing a business name shows the database name and host that would be
created, using the same slug rules (`TenantSlugs`) the server applies. It is a preview only; the server still decides.

## Layout

```
src/
├── app/
│   ├── (auth)/          login, signup — redirects away when already signed in
│   ├── (app)/           everything behind a session; the layout guards it once
│   ├── actions/         server actions: auth, organizations
│   └── icon.svg         app icon
├── components/          Field, Input, Select, Alert, Card, Badge, SubmitButton
└── lib/                 api client, session cookies, shared types
```
