# Giving every organization its own subdomain

Registering an organization already writes its subdomain down. `TenantProvisioningService` composes it from the slug
and `application.tenant.base-domain`, and `TenantSubdomainInterceptor` reads it back out of the `Host` header on the
way in, so `namaklinik.jvm.my.id` is understood by the application from the moment the row exists.

What it is not, until the three pieces below are in place, is *reachable*. Nothing resolves the name, nothing holds a
certificate for it, and nothing in front of the application accepts it.

| Layer   | What is needed                                    | Per organization? |
|---------|---------------------------------------------------|-------------------|
| DNS     | the name resolves to the server                   | no — one wildcard |
| TLS     | a certificate valid for the name                  | no — one wildcard |
| Routing | a proxy that accepts the host and passes it along | no — one wildcard |

**Set up once with wildcards, and registration needs no DNS work at all.** That is the point of doing it this way:
`*.jvm.my.id` matches a clinic that registered a minute ago exactly as it matches one that registered last year, so
there is no API call at provisioning time to fail, retry, roll back, or leave behind an orphan record. The alternative
— creating a record per tenant through the Cloudflare API — buys nothing here, and only becomes worth its complexity
if tenants are ever to bring their own domains.

## 1. DNS at Cloudflare

Two records in the `jvm.my.id` zone:

| Type | Name  | Content              | Proxy    | TTL  |
|------|-------|----------------------|----------|------|
| `A`  | `*`   | the server's IPv4    | DNS only | Auto |
| `A`  | `@`   | the server's IPv4    | DNS only | Auto |

Add `AAAA` records with the same names if the server has IPv6.

**DNS only — the grey cloud, not the orange one.** Cloudflare does not proxy wildcard records except on Enterprise
plans; leaving the toggle on gives a record that silently fails to resolve for the tenants that need it most. The apex
may be proxied if something else on it wants to be, but keep the wildcard grey.

Confirm before going further, using a name that has never existed:

```bash
dig +short anythingatall.jvm.my.id
```

Any answer at all means the wildcard is live — that is the whole test, and it is why no later step is per tenant.

## 2. The certificate

A wildcard certificate can only be issued over the **DNS-01** challenge. HTTP-01 proves one name at a time by serving
a file from it, and there is no host to serve anything from for a clinic that has not registered yet.

```bash
apt install certbot python3-certbot-nginx python3-certbot-dns-cloudflare

install -m 600 deploy/certbot/cloudflare.ini.example /etc/letsencrypt/cloudflare.ini
$EDITOR /etc/letsencrypt/cloudflare.ini     # paste the API token; see the comments in the file

certbot certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials /etc/letsencrypt/cloudflare.ini \
  --dns-cloudflare-propagation-seconds 30 \
  -d 'jvm.my.id' -d '*.jvm.my.id' \
  --cert-name jvm.my.id \
  -m you@example.com --agree-tos --no-eff-email
```

Both names, in one certificate: **a wildcard does not cover the apex it descends from**, so `*.jvm.my.id` alone would
leave `https://jvm.my.id` broken. It does not cover a second level either — `a.b.jvm.my.id` is outside it — which
matches the backend, where a host with two labels carries no tenant.

Renewal is already installed as a systemd timer by the package; it needs one addition, so that a renewed certificate
is actually the one nginx is holding:

```bash
mkdir -p /etc/letsencrypt/renewal-hooks/deploy
cat > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh <<'EOF'
#!/bin/sh
systemctl reload nginx
EOF
chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh

certbot renew --dry-run          # exercises DNS-01 against the staging server
systemctl list-timers 'certbot*'
```

Let's Encrypt allows 5 duplicate certificates per week, so rerun the issuing command sparingly; the dry run has no such
limit and is the one to iterate on.

## 3. nginx

[`deploy/nginx/jvm.my.id.conf`](../deploy/nginx/jvm.my.id.conf) is the whole of it: one `server` block whose
`server_name` is `jvm.my.id *.jvm.my.id`, proxying to Next.js on `127.0.0.1:3000`.

```bash
cp deploy/nginx/jvm.my.id.conf /etc/nginx/sites-available/
ln -s /etc/nginx/sites-available/jvm.my.id.conf /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default        # its catch-all would answer for tenant hosts
nginx -t && systemctl reload nginx
```

Two lines in it carry the weight:

* `proxy_set_header Host $host` — the tenant *is* the host name. Passing the upstream's name instead erases which
  clinic was asked for before the application ever sees the request.
* `proxy_set_header X-Tenant ""` — the backend accepts that header in place of a subdomain, which is what makes local
  development possible without wildcard DNS. On a public port it would let a caller name a tenant that the host name
  did not, so the client's version is dropped at the edge.

The Spring Boot API has no `server` block. The browser never calls it: Next.js reaches it server side over
`BACKEND_URL`, which is how the access token stays in an httpOnly cookie. Keep port 8080 bound to the loopback
interface.

## 4. The application

The pages are addressed by path — `/organizations/{slug}/people` — so a subdomain that merely resolves would land on
the root and show whatever that renders, which is not the clinic that was asked for. `frontend/src/proxy.ts` reads the
host back into a slug and redirects to the canonical URL for it, which makes `namaklinik.jvm.my.id` a front door
rather than a decoration.

It needs one environment variable, and does nothing without it:

```bash
APP_ORIGIN=https://app.jvm.my.id
```

`app` is on the reserved list, so no organization can ever take that host from under the application.

**A redirect, deliberately, rather than a rewrite.** Serving the app *from* every tenant host would keep the pretty
name in the address bar, but the session cookies are httpOnly and scoped to the host that set them: it would mean
widening them to `.jvm.my.id`, so one cookie is shared by every tenant's origin, and adding a way to sign in on each
host. That is a real feature with real isolation questions, not a configuration change, and this leaves it unmade.

## 5. What registration does now

Nothing extra — that is the outcome being aimed at. Registering "Nama Klinik" creates the database, migrates it, opens
its pool, writes `namaklinik.jvm.my.id` into the registry, and the name is already served by the wildcard that was set
up once. There is no DNS call in the request path, so there is no new way for registration to fail.

## Verifying

```bash
# resolves, without ever having been created
dig +short namaklinik.jvm.my.id

# the certificate covers it
openssl s_client -connect jvm.my.id:443 -servername namaklinik.jvm.my.id </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -ext subjectAltName

# and the request lands on that organization
curl -sI https://namaklinik.jvm.my.id/ | grep -i '^location'
```

The last one answers `location: https://app.jvm.my.id/organizations/namaklinik`.

## If a name ever does need its own record

Custom domains — a clinic pointing `klinikku.com` at this application — are the case a wildcard cannot serve, because
the name is in somebody else's zone. That needs a per-tenant path: a `custom_domain` column, a `DnsProvider` issuing
the record, on-demand certificate issuance for a name the wildcard does not cover, and an ownership check before any
of it. None of that is required for `*.jvm.my.id`, and none of it is here.
