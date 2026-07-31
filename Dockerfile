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

# Runs unprivileged: nothing here needs root.
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --home /app app

COPY --from=build --chown=app:app /build/extracted/dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/application/ ./

USER app

EXPOSE 8080

# Leaves heap sizing to the container limits rather than a fixed value.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
