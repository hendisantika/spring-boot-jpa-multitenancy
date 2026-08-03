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
| `/account`              | Your own account: the photo, and the address you sign in with            |
| `/organizations/new`    | The registration form; creates the database and the subdomain             |
| `/organizations/[slug]` | Profile, the people in it, and inviting people when you are the `OWNER`   |
| `/organizations/[slug]/edit` | Edit the profile; owner only                                        |
| `/organizations/[slug]/people` | The tenant's own people, searched and paged from `?q=` and `?page=` |
| `/organizations/[slug]/units` | The tenant's business units, searched and paged the same way        |
| `/invitations/[token]`  | Open: accept an invitation, choosing your own password                   |
| `/forgot-password`      | Ask for a reset link                                                     |
| `/reset-password/[token]` | Open: choose a new password                                            |
| `/verify-email/[token]` | Open: confirm an email address                                           |
| `/confirm-email/[token]` | Open: confirm a new address, which is when the change takes effect      |

## How it is put together

**Photos go through a server action, so its body limit is the narrowest gate.** The default is 1 MB, which
contradicted every other limit here — the backend takes a 5 MB file — and a 2 MB photo failed with an unhandled
runtime error rather than a message. `serverActions.bodySizeLimit` in `next.config.ts` is now 6 MB: the limit covers
the whole multipart body, so the boundaries and the other fields have to fit beside the file.

All three forms that take a photo share one `PhotoField`, which previews the chosen file and refuses one over 5 MB
before anything is uploaded. That is a courtesy — the API refuses an oversized file anyway — but without it the
mistake costs a full upload before anybody hears about it.

The header avatar links to `/account`, where the photo can be changed or removed — signup could set one and nothing
could afterwards. Saving revalidates the whole layout rather than the route, because the header on every page shows it.

**The email is on the same page, and the screen says up front that nothing changes until the link is confirmed.** That
is the one thing worth knowing before typing: the address is the credential, so the change waits on a link sent to the
new mailbox, which makes a typo cost an email rather than the account. While one is waiting the card says which
address and that you still sign in as the old one, with a button to cancel — an invisible pending change would look
like the form had done nothing. `pendingEmail` comes from `/api/auth/me`; the state the action returns wins over it,
so the notice appears on submit rather than only after a reload.

**The header shows the account's photo**, from `currentAccount()` — wrapped in React's `cache`, so the header and the
dashboard asking for the same account costs one request per render rather than two. It cannot be kept in a cookie
instead: the URL is signed and expires. It answers null rather than throwing, because it is called from the layout and
a failure there would take down every page under it; the header then falls back to the first letter of the email,
which the cookie still has.

The header shows `account.email` and falls back to the cookie, not the other way round. The cookie is written at
sign-in and outlives it, so once an address can change, preferring it would leave the header showing the old one for
the fortnight the session lasts.

**Stored photos are shown from a signed URL.** The bucket is private, so `photoUrl` in an API response is a presigned
`GET` that expires — 15 minutes by default. Pages are rendered per request with `cache: "no-store"`, so each render
gets a fresh one; a tab left open past the lifetime needs a reload before its images load again. They are plain `img`
tags rather than `next/image`: the host is whatever the bucket is configured as, and the URL changes every render, so
there is nothing to optimise or cache against.

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

**Searching, filtering and paging are URLs, not state.** Both list screens read `?q=`, `?page=` and one parameter per
filter, so a result can be bookmarked and the back button means what it says. The search box and the filter dropdowns
are one plain GET form — one rather than several, because applying a filter must not throw away what was typed in the
box — and it works before any JavaScript loads. There is an **Apply** button rather than a submit-on-change, so several
filters can be set before the page reloads once.

**Every filter takes several at once**, as checkboxes sharing one name — which is how HTML has always submitted a
repeated parameter, so it still needs no JavaScript. A `<select multiple>` would have been shorter to write, but
choosing several from one means knowing to hold a modifier key, and clicking without it silently throws the rest away.
Because they are all the same shape there is one filter control rather than two kinds; `ReferenceSelect` is now only
what the add/edit forms use.

Past a dozen options the checkboxes fold into a `<details>` whose summary says how many are chosen: 38 provinces would
otherwise own the page, while putting 8 blood types behind a click would cost a click and hide what is already
legible. So the provinces fold and the shorter lists do not.

All of it survives an edit: saving lands you back on the same search, filters and page, and deleting the last row of
the last page steps back rather than leaving you staring past the end. The screens share one `ListingControls`, one
`ReferenceSelect`, one `ReferenceCheckboxes`, one `Pager` and one set of URL rules (`lib/listing.ts`), so they cannot
behave differently for no reason.

Searching follows reading rather than writing: on the units screen a `MEMBER` gets the search box but not the form,
because a list you cannot narrow is a list you cannot use.

**The dropdowns come from the tenant, not from the code.** Gender, marital status, blood type and identity document on
the people form — kind of unit, operating status and province on the units form — are filled from that tenant's own
`reference-data`, fetched alongside the page in the same round trip. Both forms use one `ReferenceSelect`, which
stores a code and only ever shows a label, so a value switched off after a record was written is still shown, marked
`(no longer offered)`, rather than silently dropping out of the form. None of this is a check: the API refuses an
unknown code whatever the page offers.

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
