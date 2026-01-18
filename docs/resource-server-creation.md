# Resource Server: Create From Scratch

This document describes how to create a Quarkus-based resource server from
scratch for the oauth2-playbook examples. It focuses on required extensions,
TLS setup for local testing, OpenID Connect configuration, and scope-based
authorization (example `ScopesAllowed` annotation plus a request filter).

## Overview

- Build a small Quarkus app that validates incoming access tokens issued by
  your local OAuth2 provider (Keycloak in these examples).
- Expose HTTPS endpoints (default example uses port `8443`).
- Validate scopes in requests and abort with `403` when scopes are missing.

## Prerequisites

- Java 11+ (or the Java version supported by your Quarkus setup)
- Maven (or use the provided `mvnw` wrapper)
- Quarkus CLI (optional but convenient):

```bash
# macOS (Homebrew)
brew install quarkusio/tap/quarkus

# verify
quarkus --version
```

## Create the project skeleton

Use the Quarkus CLI to create a minimal app with the required extensions:

```bash
quarkus create app resource-server \
  --extensions='resteasy,resteasy-jackson,quarkus-oidc' \
  --no-code

cd resource-server
```

Or use Maven directly:

```bash
mvn io.quarkus:quarkus-maven-plugin:create \
  -DprojectGroupId=io.example \
  -DprojectArtifactId=resource-server \
  -Dextensions='resteasy,resteasy-jackson,quarkus-oidc'
```

## Important dependencies / extensions

- `quarkus-oidc` — validates JWTs and integrates OIDC.
- `quarkus-resteasy` and `quarkus-resteasy-jackson` — create JSON REST endpoints.

These are added by the Quarkus CLI when you pass the extensions above. If
editing `pom.xml` manually, add the matching `quarkus-bom` entries and
dependencies.

## TLS for local development

The examples use HTTPS for the resource server. Create a self-signed cert for
local testing and configure Quarkus to use it.

Create cert and key (example using OpenSSL):

```bash
mkdir -p src/main/resources/certs
openssl req -x509 -newkey rsa:2048 \
  -keyout ./src/main/resources/certs/key.pem \
  -out ./src/main/resources/certs/cert.pem \
  -days 365 -nodes \
  -subj "/C=GB/ST=England/L=Manchester/O=example/OU=dev/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

You can convert the PEM to a Java keystore if you prefer (example using
`keytool` / `openssl`). Alternatively Quarkus supports PEM on newer versions.

## `application.properties` (example)

Put the following in `src/main/resources/application.properties` (adjust as
needed for your environment):

```properties
# Server port and TLS (adjust if using keystore instead)
quarkus.http.port=8443
quarkus.http.ssl-port=8443
# If using a JVM keystore: quarkus.http.ssl.certificate.key-store-file=certs/keystore.jks
#quarkus.http.ssl.certificate.key-store-password=changeit

# OIDC (Keycloak) - use your realm URL
quarkus.oidc.enabled=true
quarkus.oidc.auth-server-url=https://localhost:9443/realms/oauth2-playbook
# Resource server typically acts as a resource (no client secret needed for token validation)
quarkus.oidc.client-id=oauth2-playbook-resource-server
# For local dev, you may relax TLS verification (not for production)
quarkus.oidc.tls.trust-all=true

# Logging, optional
quarkus.log.category."io.example".level=INFO

# CORS settings - only required for browser-based clients (e.g. React app)
quarkus.http.cors.origins=https://localhost:3000
quarkus.http.cors.enabled=true
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS
quarkus.http.cors.headers=Authorization,Content-Type
quarkus.http.cors.access-control-allow-credentials=true
quarkus.http.cors.exposed-headers=Content-Disposition 
quarkus.http.cors.access-control-max-age=24H
```

## Scope-based authorization

For fine-grained access control, use a custom annotation and a request filter
that checks the `scope` claim in the JWT. Example annotation:

```java
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopesAllowed {
    String[] value();
}
```

Example request filter (`AccessController`) that enforces the annotation:

```java
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Provider
@Priority(100)
public class AccessController implements ContainerRequestFilter {
    private final JsonWebToken jwt;
    private final ResourceInfo resourceInfo;

    public AccessController(ResourceInfo resourceInfo, JsonWebToken jwt) {
        this.resourceInfo = resourceInfo;
        this.jwt = jwt;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        ScopesAllowed annotation = resourceInfo.getResourceMethod().getAnnotation(ScopesAllowed.class);
        if (annotation == null) return;

        String[] required = annotation.value();
        String jwtScopes = jwt.getClaim("scope");
        if (jwtScopes == null || !hasRequiredScope(jwtScopes, required)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("Forbidden: JWT does not have required scopes.")
                .build());
        }
    }

    private boolean hasRequiredScope(String jwtScopes, String[] required) {
        for (String r : required) if (jwtScopes.contains(r)) return true;
        return false;
    }
}
```

Notes:
- Register the filter as a CDI bean (annotate `@Provider`) or let Quarkus
  auto-discover it.
- Tune `@Priority` if you require ordering relative to authentication.

## Example resource using `@ScopesAllowed`

```java
@Path("/api/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

  @POST
  @ScopesAllowed({"users:admin"})
  public Response create(UserDto dto) { ... }

  @GET
  @Path("/{id}")
  @ScopesAllowed({"users:read"})
  public UserDto get(@PathParam("id") String id) { ... }
}
```

## Running the server

Development run (Quarkus):

```bash
./mvnw quarkus:dev
# or, with Quarkus CLI
quarkus dev
```

Access endpoints at `https://localhost:8443/api/...`.

## Testing with a token

Once your OAuth2 provider (Keycloak) is running and you have an access token
with the required scope, call the API:

```bash
curl -k -H "Authorization: Bearer <ACCESS_TOKEN>" https://localhost:8443/api/user/123
```

For MTLS or client cert examples adjust `curl` flags accordingly (`--cert` and
`--key`).

## Tips and troubleshooting

- For local dev only: `quarkus.oidc.tls.trust-all=true` can avoid TLS trust
  issues when Keycloak uses self-signed certs.
- Keep scope names consistent between the OAuth2 provider and the resource server.
- If tokens are JWTs, inspect them with `jq`/`jwt` tools to verify `scope`.

## Further reading

- Quarkus OIDC guide: https://quarkus.io/guides/security-oidc-configuration-properties-reference
- Keycloak docs for client configuration and protocol endpoints.

---
Place this file somewhere central (for example `docs/resource-server-creation.md`) and
link from each grant type folder README so developers can follow a single,
consistent process for creating the resource server used by examples.
