# The Next.js front end. Built with bun, to match bun.lock and the CI workflow,
# and run on Node, which is what the standalone server.js is generated for.
#
# Built by .github/workflows/deploy-dev.yml and pushed to Docker Hub; the dev
# server only ever pulls it. To build it by hand:
#
#   docker build -t multitenancy-frontend frontend/
FROM oven/bun:1.3.13 AS deps

WORKDIR /app

# Dependencies before sources, so editing a page does not reinstall them.
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile


FROM oven/bun:1.3.13 AS build

WORKDIR /app

COPY --from=deps /app/node_modules node_modules
COPY . .

# NEXT_PUBLIC_ values are inlined into the client bundle at build time, so this
# one is a build argument and not an environment variable: setting it at
# runtime would change nothing. It only feeds the subdomain preview on the
# registration form; the backend remains the authority on what is created.
ARG NEXT_PUBLIC_TENANT_BASE_DOMAIN=jvm.my.id
ENV NEXT_PUBLIC_TENANT_BASE_DOMAIN=$NEXT_PUBLIC_TENANT_BASE_DOMAIN

# Selects the standalone build; see the comment in next.config.ts.
ENV NEXT_OUTPUT=standalone
ENV NEXT_TELEMETRY_DISABLED=1

RUN bun run build


# Runtime: Node, no bun, no sources, no node_modules beyond what was traced.
FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
# Without this the server binds to localhost inside the container, where
# nothing outside it can reach.
ENV HOSTNAME=0.0.0.0

# The standalone output already excludes everything not reachable from the
# server. Static assets are not part of it and have to come across separately;
# there is no public/ directory in this project, so that is the whole of it.
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static

USER node

EXPOSE 3000

# wget rather than curl: busybox ships it, and the image has no package manager
# worth waking for a health check. "/" redirects, so ask for the page it
# redirects to and require a 200.
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:3000/login || exit 1

CMD ["node", "server.js"]
