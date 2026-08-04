# A walk through the service

Nineteen screens, in the order somebody meets them: from having no account at all to reading a tenant's own
reference lists. Every picture is of the running application against a real database — none of them is a mockup,
and none was retouched. [How they were taken](#how-these-were-taken) is at the bottom, along with how to make them
again.

The tenant in the pictures is **Klinik Sehat Nusantara**, with four business units, twelve people, four members and
three unaccepted invitations. It is seeded and thrown away by the script; it is not part of the application.

---

## Getting in

### 1. Create an account

An account is not an organization. It is the parent login, and it exists before any tenant does — which is why the
service can let one person belong to several clinics without a second password.

The photo is optional here and can be added later.

![Signup](docs/screenshots/01-signup.png)

### 2. Sign in

`/` does not have a page of its own; it sends you here. Note what the subtitle says: your organizations come from
the account, not from the address you typed to arrive.

![Sign in](docs/screenshots/02-login.png)

### 3. Read the password back

Every password box in the service carries an eye. It matters most on the boxes you cannot check any other way —
choosing one at signup, confirming a new one — but it is on the sign-in box too, because "wrong password" and
"stray capital" look identical until you can see the field.

Each box keeps its own state, so revealing a new password does not uncover the current one sitting beside it.

![Password revealed](docs/screenshots/03-password-revealed.png)

### 4. Forgetting it

The reply is the same whether or not the address has an account. That is deliberate: a different message would turn
this form into a way of asking which addresses are registered.

![Forgot password](docs/screenshots/04-forgot-password.png)

---

## Your organizations

### 5. The dashboard

Only the organizations this account belongs to. Each carries the role held in it, and each has its own database and
its own subdomain — the card says both, because in this service they are not an implementation detail.

![Dashboard](docs/screenshots/05-dashboard.png)

### 6. Registering one

Registering an organization provisions a database for it and runs the tenant migrations into it. The address must be
confirmed first: provisioning a database on an unproved address is how junk tenants get created.

![Register organization](docs/screenshots/06-register-organization.png)

### 7. Inside the organization

The busiest screen in the service, and the hub for everything below it. Down the left, the profile and summaries of
the tenant's own data; down the right, the people who can sign in — members, invitations, and the forms that change
them.

Members and invitations each page, search and filter independently. The photo has a form of its own rather than
living behind Edit, so changing a picture does not mean re-submitting eight fields that were fine as they were.

![Organization](docs/screenshots/07-organization.png)

### 8. Editing the profile

The same fields registration asked for, with the photo lifted out into a form of its own back on the organization
page. The slug rides along as a hidden field — it identifies which organization is being saved, and it is not up for
editing here: it and the database name are what the routing and the connections are built on.

![Edit organization](docs/screenshots/08-organization-edit.png)

### 9. One member

A membership, not an account. The card on the organization page has room for an address and a role, which leaves the
phone number somebody would actually ring, and since when this person has had access, nowhere to live.

The links at the bottom step to the members either side, in the order the list showed them.

![Member](docs/screenshots/09-member.png)

### 10. One invitation

Who sent it, when, whether the address already has an account, and — for a pending one past its date — that it has
quietly stopped working. Expired is not a separate status: the row still says PENDING to the database while being
useless to whoever holds the link, so the badge says EXPIRED and the Withdraw button stays.

The accept link is not shown and cannot be. Only a hash of it is stored, so it exists in the recipient's mailbox and
nowhere else.

![Invitation](docs/screenshots/10-invitation.png)

### 11. Accepting one

What the person on the other end sees. They choose their own password, so nobody — including whoever invited them —
ever handles it. If the address already has an account, this screen grants access instead of asking for a password.

![Accept invitation](docs/screenshots/11-accept-invitation.png)

---

## The tenant's own data

Everything below this line lives in the tenant's own database. Nothing here is shared with another tenant, and the
lists behind the dropdowns are the tenant's own copy.

### 12. People

Twelve records, ten to a page. The search box widens — one term is matched against name, email, mobile, gender and
blood type — while every filter below it narrows, and several ticks within one filter mean "any of these".

Adding someone sits beside the list rather than behind a button, because adding is the common case on this screen.

![People](docs/screenshots/12-people.png)

### 13. Narrowed to one unit

The same list with a unit ticked. The count and the paging follow the filter rather than the whole list, and the URL
carries the filter — so a narrowed list is a link somebody can send.

![People filtered](docs/screenshots/13-people-filtered.png)

### 14. One person

The record whole, including the fields the row has no room for. The unit is a link, and it leads to everyone else in
that unit rather than to the unit's own page — from one person, the next question is usually their colleagues. The
step at the bottom walks the list in the order it was showing.

![Person](docs/screenshots/14-person.png)

### 15. Business units

The clinics themselves — a main clinic, two branches and a satellite that has not opened yet. Operating status is
part of the record, so "opening soon" is something the data says rather than something a reader infers from an empty
list of people.

![Units](docs/screenshots/15-units.png)

### 16. One unit

Address, province, contact address and status, plus a way through to the people attached to this unit — which is the
question anybody reading a unit asks next.

![Unit](docs/screenshots/16-unit.png)

### 17. Reference lists

The lists behind every dropdown: blood types, provinces, marital statuses, and eight more. Each tenant gets its own
copy from a migration, so renaming a label here would not rewrite anybody's record — a record stores the code.

![Reference data](docs/screenshots/17-reference-data.png)

### 18. One reference list

Thirty-eight provinces, paged and searchable. Both the label and the code are shown, because the label is what gets
read and the code is what a record actually holds and an API caller has to send.

These are read-only through the API on purpose. Changing what a list holds means a new migration, never an edit to an
applied one, or every tenant database fails its checksum.

![Reference list](docs/screenshots/18-reference-list.png)

---

## Your account

### 19. Settings

One account, one screen: photo, phone, password and email address, each with its own form so saving one never means
re-submitting the others.

Two of them are deliberately awkward. Changing the password signs out every other session, because a password is
changed when somebody else may know it. Changing the email address takes the current password and does nothing until
a link sent to the *new* address is opened — so a mistyped address costs an email rather than the account.

![Account](docs/screenshots/19-account.png)

---

## How these were taken

`frontend/scripts/screenshots.mjs` drives a real Chromium through Playwright. It seeds its own tenant through the
API first, because a tour of empty screens teaches nothing: a members list with one row does not show that it pages,
and a people list with three does not show that it filters.

```bash
# 1. The backend, with no BREVO_API_KEY set.
#    Accounts are then verified at signup and invitations hand the link back
#    instead of mailing it, so the tour needs no mailbox and mails nobody.
BREVO_API_KEY= java -jar target/multitenancy-0.0.1-SNAPSHOT.jar --server.port=8081

# 2. The frontend, built rather than in dev — the dev overlay lands in the pictures.
cd frontend && npm run build
BACKEND_URL=http://localhost:8081 npx next start -p 3001

# 3. The tour.
BACKEND_URL=http://localhost:8081 APP_URL=http://localhost:3001 node scripts/screenshots.mjs
```

Chromium comes from `npx playwright install chromium`, once.

The pictures are 1280 wide at 2× and full-page, so a long screen is one tall image rather than a fold. Locale is
fixed to `en-GB` and the clock to `Asia/Jakarta`, or the dates in the pictures would not match the application
anybody here is running.

### Removing the tour data

```bash
cd frontend && BACKEND_URL=http://localhost:8081 node scripts/screenshots.mjs --clean
```

That withdraws the invitations and deletes the people and units. The tenant row, its database and the four accounts
survive it, because no endpoint removes them — dropping a database is not a decision a screenshot script should make
on its own. To finish the job by hand:

```sql
DELETE FROM db_default.invitations  WHERE tenant_slug = 'kliniksehatnusantara';
DELETE FROM db_default.user_tenants WHERE tenant_slug = 'kliniksehatnusantara';
DELETE FROM db_default.tenants      WHERE slug        = 'kliniksehatnusantara';
DELETE v FROM db_default.email_verifications v
  JOIN db_default.accounts a ON a.id = v.account_id WHERE a.email LIKE '%@example.com';
DELETE FROM db_default.accounts     WHERE email LIKE '%@example.com';
DROP DATABASE kliniksehatnusantara;
```

### Taking them again

Re-running against a tenant the script already made reuses what is there. One step cannot be repeated: an accept link
is handed back only at the moment the invitation is created, so a second run has no link to photograph and says so
rather than dropping step 11 silently. For a faithful set, clear the tour data first.
