# Build stage. Dependencies are resolved before the sources are copied, so
# editing code does not re-download the world on every build.
FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# The tests need a MySQL and an S3 endpoint, which an image build has neither
# of; CI is what runs them.
RUN ./mvnw -B -q clean package -DskipTests

# Split the jar into layers that change at different rates, so a code change
# does not invalidate the dependency layer. The launcher jar keeps the project
# version in its name, so normalise it here and the runtime stage stays stable
# across releases.
RUN java -Djarmode=tools -jar target/multitenancy-*.jar extract --layers --destination extracted \
    && mv extracted/application/multitenancy-*.jar extracted/application/app.jar


# Runtime stage: a JRE, no build tooling.
FROM eclipse-temurin:25-jre

WORKDIR /app

# curl is only here for HEALTHCHECK; the base image ships neither it nor wget.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Runs unprivileged: nothing here needs root.
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --home /app app

COPY --from=build --chown=app:app /build/extracted/dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/application/ ./

USER app

EXPOSE 8080

# The commit and branch this image was built from. The build cannot read them
# itself — .git is not in the context — so CI passes them, and they surface at
# runtime in /actuator/info. They default to "unknown" for a plain `docker
# build` with no arguments, which is the truthful answer then.
ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ENV GIT_COMMIT=$GIT_COMMIT
ENV GIT_BRANCH=$GIT_BRANCH

# Leaves heap sizing to the container limits rather than a fixed value.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# The overall health group, not /health/readiness: the default readiness group
# contains only readinessState and stays UP with the database unreachable, which
# was confirmed by pulling the network from a running container. The overall
# group includes the database, so "healthy" means the application can really
# serve. start-period covers boot and the Flyway migrations and is not counted
# as failure time.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
