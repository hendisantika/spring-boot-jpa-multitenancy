/**
 * Takes the pictures in SCREENSHOTS.md.
 *
 * It seeds its own tenant through the API first, because a tour of empty
 * screens teaches nothing: a members list with one row does not show that it
 * pages, and a people list with three does not show that it filters. Everything
 * it creates is named after TOUR, so `node scripts/screenshots.mjs --clean`
 * can find it again.
 *
 * Run the backend with no BREVO_API_KEY. Accounts are then verified at signup
 * and invitations hand the link back instead of mailing it, so the tour needs
 * no mailbox and sends nobody a real email.
 *
 *   BACKEND_URL=http://localhost:8081 APP_URL=http://localhost:3001 \
 *     node scripts/screenshots.mjs
 */

import { mkdir, readdir, rm } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { chromium } from "playwright";

const API = process.env.BACKEND_URL ?? "http://localhost:8081";
const APP = process.env.APP_URL ?? "http://localhost:3001";
const OUT = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "docs", "screenshots");

const TOUR = {
  email: "tour.owner@example.com",
  password: "tour-password-123",
  businessName: "Klinik Sehat Nusantara",
  slug: "kliniksehatnusantara",
};

async function call(path, { token, tenant, json, method = "GET", form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (tenant) headers["X-Tenant"] = tenant;
  if (json !== undefined) headers["Content-Type"] = "application/json";

  const response = await fetch(`${API}${path}`, {
    method: json !== undefined || form ? "POST" : method,
    headers,
    body: form ?? (json === undefined ? undefined : JSON.stringify(json)),
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(`${method} ${path} → ${response.status} ${text.slice(0, 200)}`);
  }
  return body;
}

/** A token carries the memberships held when it was issued, so this is called
 *  again after the organization exists — the one from before does not have it. */
async function logIn() {
  const { accessToken } = await call("/api/auth/login", {
    json: { email: TOUR.email, password: TOUR.password },
  });
  return accessToken;
}

async function seed() {
  const account = new FormData();
  account.append(
    "account",
    new Blob(
      [JSON.stringify({ email: TOUR.email, phoneNumber: "+62 812 1000 2000", password: TOUR.password })],
      { type: "application/json" },
    ),
  );
  const signup = await fetch(`${API}/api/auth/signup`, { method: "POST", body: account });
  // 409 means a previous run left it behind, which is fine — the tour is the
  // same either way, and re-running should not need a teardown first.
  if (!signup.ok && signup.status !== 409) {
    throw new Error(`signup → ${signup.status} ${(await signup.text()).slice(0, 200)}`);
  }

  let token = await logIn();
  const mine = await call("/api/organizations", { token });
  let slug = mine.find((o) => o.slug === TOUR.slug)?.slug;

  if (!slug) {
    const organization = new FormData();
    organization.append(
      "organization",
      new Blob(
        [
          JSON.stringify({
            businessName: TOUR.businessName,
            businessEmail: "halo@sehatnusantara.example",
            contactFirstName: "Sari",
            contactLastName: "Wijaya",
            jobTitle: "Practice Manager",
            phoneNumber: "+62 812 1000 2000",
            orgStructure: "MULTI_LOCATION_CLINIC",
            practiceSpeciality: "AESTHETIC_AND_DERMA",
          }),
        ],
        { type: "application/json" },
      ),
    );
    const created = await fetch(`${API}/api/organizations`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: organization,
    });
    if (!created.ok) throw new Error(`organization → ${created.status}`);
    slug = (await created.json()).slug;
    token = await logIn();
  }

  const units = await seedUnits(token, slug);
  await seedPeople(token, slug, units);
  const members = await seedMembers(token, slug);
  const invitations = await seedInvitations(token, slug);

  return { token, slug, units, members, invitations };
}

const UNITS = [
  { name: "Klinik Pusat Menteng", unitType: "MAIN_CLINIC", operatingStatus: "OPEN",
    address: "Jl. Cikini Raya 12, Jakarta Pusat", province: "DKI_JAKARTA", email: "menteng@sehatnusantara.example" },
  { name: "Cabang Bandung Dago", unitType: "BRANCH_CLINIC", operatingStatus: "OPEN",
    address: "Jl. Ir. H. Juanda 88, Bandung", province: "JAWA_BARAT", email: "dago@sehatnusantara.example" },
  { name: "Cabang Surabaya Darmo", unitType: "BRANCH_CLINIC", operatingStatus: "OPEN",
    address: "Jl. Raya Darmo 45, Surabaya", province: "JAWA_TIMUR", email: "darmo@sehatnusantara.example" },
  { name: "Satelit Yogyakarta", unitType: "SATELLITE", operatingStatus: "OPENING_SOON",
    address: "Jl. Malioboro 7, Yogyakarta", province: "DI_YOGYAKARTA", email: "jogja@sehatnusantara.example" },
];

async function seedUnits(token, tenant) {
  const existing = await call("/organization?size=50", { token, tenant });
  if (existing.content.length >= UNITS.length) return existing.content;
  const made = [];
  for (const unit of UNITS) {
    made.push(await call("/organization", { token, tenant, json: unit }));
  }
  return made;
}

const PEOPLE = [
  ["Dewi", "Anggraini", "FEMALE", "MARRIED", "O_POSITIVE", "1990-08-17"],
  ["Budi", "Santoso", "MALE", "MARRIED", "A_POSITIVE", "1985-03-02"],
  ["Ratna", "Kusuma", "FEMALE", "SINGLE", "B_POSITIVE", "1996-11-24"],
  ["Agus", "Prasetyo", "MALE", "SINGLE", "AB_POSITIVE", "1993-05-09"],
  ["Siti", "Rahayu", "FEMALE", "MARRIED", "O_NEGATIVE", "1988-01-30"],
  ["Joko", "Susilo", "MALE", "DIVORCED", "A_NEGATIVE", "1979-07-14"],
  ["Maya", "Lestari", "FEMALE", "SINGLE", "B_NEGATIVE", "1998-09-05"],
  ["Rizky", "Hidayat", "MALE", "MARRIED", "O_POSITIVE", "1991-12-19"],
  ["Nurul", "Aisyah", "FEMALE", "MARRIED", "AB_NEGATIVE", "1987-04-22"],
  ["Bayu", "Nugroho", "MALE", "SINGLE", "A_POSITIVE", "1994-10-11"],
  ["Indah", "Permata", "FEMALE", "WIDOWED", "B_POSITIVE", "1976-02-08"],
  ["Fajar", "Ramadhan", "MALE", "SINGLE", "O_POSITIVE", "1999-06-27"],
];

async function seedPeople(token, tenant, units) {
  const existing = await call("/person?size=1", { token, tenant });
  if (existing.totalElements >= PEOPLE.length) return;
  let index = 0;
  for (const [firstName, lastName, gender, maritalStatus, bloodType, birthDate] of PEOPLE) {
    await call("/person", {
      token,
      tenant,
      json: {
        firstName, lastName, gender, maritalStatus, bloodType, birthDate,
        identityDocumentType: "KTP",
        identityNumber: `31710657089${String(index).padStart(5, "0")}`,
        mobile: `+62 813 5555 ${String(1000 + index)}`,
        email: `${firstName}.${lastName}`.toLowerCase() + "@example.com",
        // Spread across the units so the unit filter has something to narrow.
        organization: { id: units[index % units.length].id },
      },
    });
    index += 1;
  }
}

const MEMBERS = [
  { email: "arif.wibowo@example.com", phoneNumber: "+62 813 2000 0001", role: "MEMBER" },
  { email: "citra.melati@example.com", phoneNumber: "+62 813 2000 0002", role: "MEMBER" },
  { email: "hendra.gunawan@example.com", phoneNumber: "+62 813 2000 0003", role: "OWNER" },
];

async function seedMembers(token, slug) {
  const existing = await call(`/api/organizations/${slug}/users?size=50`, { token });
  if (existing.totalElements > MEMBERS.length) return existing.content;
  for (const member of MEMBERS) {
    try {
      await call(`/api/organizations/${slug}/users`, {
        token,
        json: { ...member, password: "member-password-123" },
      });
    } catch (error) {
      if (!String(error).includes("409")) throw error;
    }
  }
  return (await call(`/api/organizations/${slug}/users?size=50`, { token })).content;
}

const INVITEES = [
  { email: "lina.marlina@example.com", role: "MEMBER" },
  { email: "yusuf.maulana@example.com", role: "MEMBER" },
  { email: "putri.handayani@example.com", role: "OWNER" },
];

/**
 * The accept link is only ever handed back at the moment of creation — only a
 * hash of it is stored — so a second run against invitations it did not create
 * cannot photograph the accept screen. It says so rather than quietly dropping
 * a step from the tour.
 */
async function seedInvitations(token, slug) {
  const already = await call(`/api/organizations/${slug}/invitations?size=50`, { token });
  if (already.totalElements >= INVITEES.length) {
    console.log("  invitations already exist; their accept links are gone, so step 05 is skipped");
    return already.content;
  }
  const made = [];
  for (const invitee of INVITEES) {
    made.push(await call(`/api/organizations/${slug}/invitations`, { token, json: invitee }));
  }
  return made;
}

/* ------------------------------------------------------------------ shots */

/**
 * The passes the tour is taken in.
 *
 * The three widths are real devices, not the narrowest and widest a browser can
 * be dragged to: the point is what a person sees, and a viewport nobody owns
 * proves nothing. They land in different Tailwind ranges on purpose — 390 below
 * `md`, 820 between `md` and `lg`, 1280 at `xl` — so each is a layout the
 * stylesheet actually describes rather than three samples of the same one.
 *
 * `isMobile` matters as much as the width on the two touch shapes: it is what
 * makes the page report a touch screen and lay itself out accordingly instead
 * of behaving like a small desktop.
 *
 * The dark pass is desktop-width only. The theme is a set of colour tokens
 * swapped by `prefers-color-scheme`, not a layout, so shooting it at all three
 * widths would be fifty-seven pictures of one palette change.
 *
 * Only the desktop pass is 2x. The other three are answering a yes-or-no
 * question — does it stack, does it go dark — and are read at a fraction of
 * their width, so a second pixel each way is detail nobody looks at in a file
 * the history then carries forever.
 */
const SHAPES = [
  {
    name: "desktop",
    dir: "",
    context: {
      viewport: { width: 1280, height: 800 },
      deviceScaleFactor: 2,
    },
  },
  {
    name: "dark",
    dir: "dark",
    context: {
      viewport: { width: 1280, height: 800 },
      deviceScaleFactor: 1,
      // There is no toggle to click: globals.css answers the media query, so
      // asking for the scheme is the whole of it.
      colorScheme: "dark",
    },
  },
  {
    name: "tablet",
    dir: "tablet",
    context: {
      // An iPad Air held upright.
      viewport: { width: 820, height: 1180 },
      deviceScaleFactor: 1,
      isMobile: true,
      hasTouch: true,
      userAgent:
        "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
    },
  },
  {
    name: "phone",
    dir: "mobile",
    context: {
      viewport: { width: 390, height: 844 },
      // 2 rather than a real phone's 3: these are read at a fraction of their
      // width in a document, and a third of the sharpness nobody sees costs
      // three megabytes that the history then keeps forever.
      deviceScaleFactor: 1,
      isMobile: true,
      hasTouch: true,
      userAgent:
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
    },
  },
];

async function shoot(page, out, name, url) {
  await page.goto(`${APP}${url}`, { waitUntil: "networkidle" });
  // Photos are signed URLs fetched after hydration; without this they land in
  // the picture as broken-image icons.
  await page.waitForTimeout(600);
  await page.screenshot({ path: join(out, `${name}.png`), fullPage: true });
  console.log(`  ${name}.png  ${url}`);
}

/**
 * One pass of the tour.
 *
 * The numbers follow the order SCREENSHOTS.md reads in, which is not the order
 * the pictures can be taken in: everything signed out has to come first. `/` is
 * not among them — it only redirects to sign-in, so its picture was the login
 * screen twice.
 */
async function tour(page, out, { slug, units, owner, people, invitations }) {
  await shoot(page, out, "01-signup", "/signup");
  await shoot(page, out, "02-login", "/login");
  await shoot(page, out, "04-forgot-password", "/forgot-password");
  // The link a pending invitation carries. With no Brevo key the API hands it
  // back rather than mailing it, which is the only reason this is reachable.
  const accept = invitations.find((i) => i.acceptUrl)?.acceptUrl;
  if (accept) await shoot(page, out, "11-accept-invitation", new URL(accept).pathname);

  await page.goto(`${APP}/login`, { waitUntil: "networkidle" });
  await page.fill('input[name="email"]', TOUR.email);
  await page.fill('input[name="password"]', TOUR.password);
  // Proof the eye works, and the only screenshot that needs a password in it.
  await page.click('button[aria-label="Show password"]');
  await page.screenshot({ path: join(out, "03-password-revealed.png"), fullPage: true });
  console.log("  03-password-revealed.png  /login");
  await page.click('button[aria-label="Hide password"]');
  await Promise.all([page.waitForURL(`${APP}/dashboard`), page.click('button[type="submit"]')]);

  await shoot(page, out, "05-dashboard", "/dashboard");
  await shoot(page, out, "06-register-organization", "/organizations/new");
  await shoot(page, out, "07-organization", `/organizations/${slug}`);
  await shoot(page, out, "08-organization-edit", `/organizations/${slug}/edit`);
  await shoot(page, out, "09-member", `/organizations/${slug}/members/${owner.accountId}`);
  await shoot(page, out, "10-invitation", `/organizations/${slug}/invitations/${invitations[0].id}`);
  await shoot(page, out, "12-people", `/organizations/${slug}/people`);
  await shoot(page, out, "13-people-filtered", `/organizations/${slug}/people?unit=${units[0].id}`);
  await shoot(page, out, "14-person", `/organizations/${slug}/people/${people[0].id}`);
  await shoot(page, out, "15-units", `/organizations/${slug}/units`);
  await shoot(page, out, "16-unit", `/organizations/${slug}/units/${units[0].id}`);
  await shoot(page, out, "17-reference-data", `/organizations/${slug}/reference-data`);
  await shoot(page, out, "18-reference-list", `/organizations/${slug}/reference-data/PROVINCE`);
  await shoot(page, out, "19-account", "/account");
}

async function main() {
  if (process.argv.includes("--clean")) return clean();

  console.log("Seeding…");
  const { token, slug, units, members, invitations } = await seed();
  const people = (await call("/person?size=1", { token, tenant: slug })).content;
  const owner = members.find((m) => m.email === TOUR.email) ?? members[0];
  console.log(`  tenant ${slug}: ${units.length} units, ${PEOPLE.length} people, ` +
    `${members.length} members, ${invitations.length} invitations`);

  const browser = await chromium.launch();
  let total = 0;

  for (const shape of SHAPES) {
    const out = join(OUT, shape.dir);
    await mkdir(out, { recursive: true });
    for (const file of await readdir(out).catch(() => [])) {
      if (file.endsWith(".png")) await rm(join(out, file));
    }

    console.log(`\nShooting ${shape.name} (${shape.context.viewport.width}px)…`);
    const context = await browser.newContext({
      ...shape.context,
      // Dates read differently per locale, and a screenshot that says 8/17/1990
      // does not match the app anybody here is running.
      locale: "en-GB",
      timezoneId: "Asia/Jakarta",
      reducedMotion: "reduce",
    });
    const page = await context.newPage();
    await tour(page, out, { slug, units, owner, people, invitations });
    await context.close();
    total += (await readdir(out)).filter((f) => f.endsWith(".png")).length;
  }

  await browser.close();
  console.log(`\n${total} screenshots in docs/screenshots/.`);
  console.log(`Remove the tour data with:  node scripts/screenshots.mjs --clean`);
}

/**
 * Undoes the seeding as far as the API allows.
 *
 * The tenant row and its database are not exposed for deletion, so they are
 * named rather than removed — dropping a database is not something a script
 * should decide to do on its own.
 */
async function clean() {
  const token = await logIn().catch(() => null);
  if (!token) return console.log("No tour account; nothing to clean.");

  const invitations = await call(`/api/organizations/${TOUR.slug}/invitations?size=100`, { token });
  for (const invitation of invitations.content) {
    if (invitation.status === "PENDING") {
      await call(`/api/organizations/${TOUR.slug}/invitations/${invitation.id}`, { token, method: "DELETE" });
    }
  }
  console.log(`Withdrew ${invitations.content.length} invitations.`);

  const people = await call("/person?size=500", { token, tenant: TOUR.slug });
  for (const person of people.content) {
    await call(`/person/${person.id}`, { token, tenant: TOUR.slug, method: "DELETE" });
  }
  const units = await call("/organization?size=100", { token, tenant: TOUR.slug });
  for (const unit of units.content) {
    await call(`/organization/${unit.id}`, { token, tenant: TOUR.slug, method: "DELETE" });
  }
  console.log(`Deleted ${people.content.length} people and ${units.content.length} units.`);

  console.log(
    `\nStill there, because no endpoint removes them:\n` +
      `  the tenant ${TOUR.slug} and its database of the same name\n` +
      `  the accounts ${[TOUR.email, ...MEMBERS.map((m) => m.email)].join(", ")}`,
  );
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
