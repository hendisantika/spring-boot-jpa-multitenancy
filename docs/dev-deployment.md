# Deploying to the dev server

A push to `main` that passes its checks builds two images, pushes them to Docker Hub and points the dev server at that
pair. There are two pipelines, one per side, each complete in itself:
[`backend.yml`](../.github/workflows/backend.yml) and [`frontend.yml`](../.github/workflows/frontend.yml), with
[`deploy/compose.dev.yaml`](../deploy/compose.dev.yaml) as what ends up running.

Each watches the paths it owns — `src/`, `pom.xml`, the `Dockerfile` and `deploy/compose.dev.yaml` on one side, `frontend/` on the
other — so a commit that only changes the front end does not wait for the Maven tests, and neither deploys until its
own tests have passed. A commit touching both runs both pipelines, and the second deploy is the same pair of images
being pulled again; the two deploy jobs share a concurrency group, so they queue rather than collide.

The deploy half of the two files is the same four jobs twice over, which is the price of each pipeline standing alone:
**a change to how deployment works has to be made in both.**

Both images are always built and always deployed together, whichever pipeline ran: the pair on the server then comes
from one commit, and there is no combination running that was never tested as one. A front end change rebuilding the
API image costs little — the build cache is keyed per image and nothing in `src/` moved.

Nothing is built on the server and nothing is configured by hand there. The `.env` beside the compose file is written
from repository secrets and variables on every deploy, so **editing it on the server changes nothing that survives the
next run** — change the secret or the variable instead.

MySQL is the server's own, reached at the address the URL template names. MinIO is an existing container on its
`minio_minio-net` network, which the compose file joins so the bucket answers at `http://minio:9000`. Redis is this
deployment's own — the box's shared Redis needs a password this app has no way to send — so the compose file brings up
an isolated one rather than depending on the host.

```
push to main ──► the pipeline whose paths changed:

    backend.yml    test ──┐
                          ├──► images (both) ──► push to Docker Hub ──► deploy ──► wait for 200s
    frontend.yml   test ──┘         ▲                                     │
                                    │                                     │
                          check settings ─────────────────────────────────┘
```

## What to set on the repository

Settings → Secrets and variables → Actions. The split is deliberate: **variables** are visible in the logs and in the
settings page, **secrets** are masked and cannot be read back, so anything that would let somebody in is a secret and
everything else is a variable — an address masked into `***` only makes a failed deploy harder to read.

### Secrets

| Name                             | Value                                                            |
|----------------------------------|------------------------------------------------------------------|
| `SSH_HOST`                       | `165.22.246.205`                                                 |
| `SSH_PORT`                       | `2280`                                                           |
| `SSH_USERNAME`                   | `deployer`                                                       |
| `SSH_PRIVATE_KEY`                | the whole private key, `-----BEGIN` line to `-----END` line      |
| `SSH_KNOWN_HOSTS`                | optional but wanted; see below                                   |
| `DOCKERHUB_TOKEN`                | an access token from Docker Hub, with Read & Write               |
| `APPLICATION_JWT_SECRET`         | `openssl rand -base64 48` — never the value in the repository    |
| `APPLICATION_DATABASE_PASSWORD`  | the MySQL password on the server                                 |
| `APPLICATION_STORAGE_ACCESS_KEY` | MinIO access key; omit both keys to use the AWS credential chain |
| `APPLICATION_STORAGE_SECRET_KEY` | MinIO secret key                                                 |
| `REDIS_PASSWORD`                 | optional; empty for an unauthenticated Redis, set to reach a protected one |
| `BREVO_API_KEY`                  | optional — without it, invitation links are returned rather than mailed |

### Variables

| Name                                      | Value                                                                    |
|-------------------------------------------|--------------------------------------------------------------------------|
| `DOCKERHUB_USERNAME`                      | `hendisantika`, or whichever account owns the images                     |
| `PUBLIC_BASE_URL`                         | where a browser reaches this deployment, e.g. `https://dev.jvm.my.id` — no trailing slash |
| `APPLICATION_DATABASE_URL_TEMPLATE`       | see below; `{database}` is substituted per tenant                        |
| `APPLICATION_DATABASE_USER`               | the MySQL user, which must be allowed to `CREATE DATABASE`               |
| `APPLICATION_DATABASE_CENTRAL_DATABASE`   | optional, defaults to `db_default`                                        |
| `APPLICATION_STORAGE_ENDPOINT`            | `http://minio:9000` — the MinIO container on the joined `minio_minio-net` network |
| `APPLICATION_STORAGE_SIGNED_URL_ENDPOINT` | the address a *browser* can reach MinIO at; a signature covers the host, so a URL signed for the internal one cannot be repointed |
| `APPLICATION_STORAGE_BUCKET`              | optional, defaults to `jvm-uploads`                                       |
| `APPLICATION_TENANT_BASE_DOMAIN`          | optional, defaults to `jvm.my.id`                                         |
| `REDIS_HOST` / `REDIS_PORT`               | `redis` and `6379` — the compose service, not a host address              |
| `REDIS_TIMEOUT`                           | optional, defaults to `10000ms`                                           |
| `REDIS_USERNAME`                          | optional; empty for an unauthenticated Redis, set to reach a protected one |
| `APP_ORIGIN`                              | same as `PUBLIC_BASE_URL`; it is what sends a tenant subdomain to its pages |
| `BREVO_SENDER_EMAIL` / `BREVO_SENDER_NAME`| the verified sender, if Brevo is configured                               |
| `BIND_ADDRESS`                            | optional, defaults to `127.0.0.1`; `0.0.0.0` publishes the ports to the internet |
| `DEPLOY_PATH`                             | optional, defaults to `multitenancy`, relative to the deploy user's home  |

The database URL template, with the host as the containers see it:

```
jdbc:mysql://host.docker.internal:3306/{database}?createDatabaseIfNotExist=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&connectionTimeZone=UTC&useSSL=false&allowPublicKeyRetrieval=true
```

`SPRING_PROFILES_ACTIVE=prod` is set by the compose file rather than by a variable, and it is not optional. Under that
profile the application refuses to start on the credentials committed to this repository — which are public, and which
would otherwise let anyone who has read it mint a token for any account on a server with an address.

### The host key

Without `SSH_KNOWN_HOSTS` the workflow accepts whatever answers on the first connection of every run, which is no
protection at all. Produce it once, from a machine you already trust to reach the server:

```bash
ssh-keyscan -p 2280 165.22.246.205
```

Paste all of the output — every line — into the secret.

## What the server needs

* Docker with the compose plugin (`docker compose version`), and `deployer` in the `docker` group.
* The deploy key's public half in `~deployer/.ssh/authorized_keys`.
* MySQL reachable from a container, with a user that may `CREATE DATABASE`: the application creates one per
  organization. If MySQL only listens on `127.0.0.1`, bind it on the Docker bridge as well, or the containers cannot
  reach it whatever the URL says.
* The bucket must already exist. Nothing creates it — locally that is the `minio-init` service, and here it is a
  one-off `mc mb`.
* `curl`, which the deploy uses to wait for the two ports to answer.

## nginx on the server

The ports are published on the loopback interface, so nginx is what reaches the internet:

```nginx
location / {
    proxy_pass http://127.0.0.1:5000;      # the front end
    proxy_set_header Host              $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Tenant          "";
}
```

The API on `8010` needs no location of its own: the browser never calls it, the front end reaches it over the compose
network, and the port is published for looking at from the server itself.
[`deploy/nginx/jvm.my.id.conf`](../deploy/nginx/jvm.my.id.conf) is the fuller version, with the wildcard for tenant
subdomains — see [wildcard-subdomain.md](wildcard-subdomain.md).

Dropping `X-Tenant` matters here as much as there: the backend accepts it in place of a subdomain, and on a public
port it would let a caller name a tenant the host name did not.

## Deploying, and undeploying

Push to `main`, and it goes once that side's tests are green. Or run either workflow by hand from Actions → Run
workflow, which is also how a rollback works: give it the tag of a build that was good, and both the tests and the
image build are skipped — it deploys what it is given without waiting for anything, which is the point of it. A
rollback happens when something is already wrong.

```
be-41                # the run number of Deploy Backend CI/CD to Dev
fe-17                # the run number of Deploy Frontend CI/CD to Dev
sha-1a2b3c4          # or the commit, which every image also carries
```

The run number is what a deploy runs and what the run itself is called, so a deployment can be matched to its build at
a glance. It counts **per workflow**, which is why it is prefixed: the two pipelines have separate counters, and an
unprefixed `41` from each would be two different commits wearing one tag, the second quietly overwriting the first.
Every image also carries `sha-<commit>`, so a tag on the server can always be traced back to source, and either form
works for a rollback.

Images carry `dev` as well, which moves with the latest deploy. Nothing is ever deployed by that tag — a deployment
that says `dev` cannot be rolled back to anything, because it does not say which build it was.

The workflow prints `docker compose ps` and the last sixty log lines at the end of every run, successful or not, so a
container that came up and died is visible without logging in.

## When it fails

| Symptom                                                | Where to look                                                            |
|--------------------------------------------------------|--------------------------------------------------------------------------|
| `Not set, see docs/dev-deployment.md: …`               | the named secret or variable is missing                                   |
| API container restarts, logs name a `${...}` placeholder | a `prod` profile variable is unset; the check is deliberate               |
| `Communications link failure`                          | MySQL is not reachable from inside a container — see the note above       |
| Front end up, every page 500                           | `BACKEND_URL` — the compose file sets it, so suspect the API being unhealthy |
| Mailed links point at `localhost`                      | `PUBLIC_BASE_URL` is unset or wrong                                       |
| `permission denied while trying to connect to the Docker daemon` | `deployer` is not in the `docker` group                        |
| `Permission denied (publickey)` on the first ssh                 | the deploy key is not in `~deployer/.ssh/authorized_keys`, or `SSH_PRIVATE_KEY` holds a different key |

The deploy prints the fingerprint of the key it is about to offer, in the *Prepare SSH* step. Compare it against what
the server accepts, and a rejected key tells its own story:

```bash
ssh-keygen -lf ~/.ssh/id_ed25519_deploy          # what you put in the secret
ssh-keygen -lf ~deployer/.ssh/authorized_keys    # what the server will take
ssh -o BatchMode=yes -i ~/.ssh/id_ed25519_deploy -p 2280 deployer@165.22.246.205 true
```

The last command answering nothing at all is the whole test. A key with a passphrase cannot be used here either —
there is nobody to type it.
