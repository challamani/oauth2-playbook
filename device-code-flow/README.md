# Device Code Flow OAuth2 Example

This scenario demonstrates the OAuth 2.0 Device Authorization Grant (device code flow) using a local OAuth2 provider (Keycloak), a resource server (Quarkus example app) and a client helper script that performs the device flow and calls the API.

- **OAuth2 Provider (Keycloak)**: runs in `device-code-flow/oauth2-provider/` and exposes the device authorization endpoint (`/protocol/openid-connect/auth/device`) and the token endpoint. It ships with test certificates and an importable realm (`imports/realm.json`). The provider listens on HTTPS port `9443` for admin and device endpoints.
- **Client**: a small helper script `device-code-flow/client/create-user.sh` which starts a device authorization request, prompts the user to visit the verification URL and enter the user code, polls the token endpoint, then calls the resource server with the obtained access token.
- **Resource Server**: a Quarkus-based example application under `device-code-flow/resource-server` (and a convenience setup script `device-code-flow/resource-server/setup-resource-server.sh`). The example app binds to HTTPS port `8443` by default and validates incoming access tokens and scopes.

Flow summary:

1. Client POSTs to the device authorization endpoint and receives `device_code`, `user_code`, and `verification_uri`.
2. User visits the `verification_uri` (on a browser-capable device) and authenticates using the `user_code`.
3. Client polls the token endpoint with `device_code` until the user authorizes and an access token is returned.
4. Client calls the resource server API with `Authorization: Bearer <access_token>` to perform operations such as creating a user.

## Structure

Top-level layout for this scenario:

- `client/` — helper client scripts. Key file: `create-user.sh`
- `oauth2-provider/` — local provider artifacts: `docker-compose.yaml`, `start.sh`, `certs/`, and `imports/realm.json`.
- `resource-server/` — helper to create or run the resource server; contains `src/` Quarkus project.

Keep reading for how to start each component.

## Start Keycloak Server (OAuth2 Provider)


### Understand the realm setup

- json: `device-code-flow/oauth2-provider/imports/realm.json`

```bash
#start the oauth2 provider
cd ./device-code-flow/oauth2-provider

#remove any existing containers related to keycloak
docker rm $(docker ps -a -q --filter name=keycloak) 2>/dev/null || true
./start.sh
```

- Login to Keycloak admin console at: `https://localhost:9443/` (admin/admin)


### Client & Realm configuration for Device Flow

Key values

- **Realm**: `oauth2-playbook` (import file: `device-code-flow/oauth2-provider/imports/realm.json`)
- **Client ID**: `oauth2-playbook-device-flow`

Required client settings (Keycloak UI or in the imported `realm.json`)

- **Public client**: ON / `publicClient: true` — device flow clients are typically public.
- **OAuth2 Device Authorization Grant**: Enabled / `attributes.oauth2.device.authorization.grant.enabled: "true"`.
- **Consent required**: true (can be toggled per your UX needs).
- **Redirect URIs**: Not required for device flow; leave blank or set to `+` in Keycloak if needed.
- **Default scopes**: include `profile`; add optional `users:admin` if the resource server requires it.

Relevant endpoints (default local URLs)

- Device Authorization Endpoint: `https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/auth/device`
- Token Endpoint: `https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/token`

Notes

- The provided `realm.json` already contains a `oauth2-playbook-device-flow` client configured for device flow and a `users:admin` scope mapped to the resource server audience. Inspect `device-code-flow/oauth2-provider/imports/realm.json` to see the exact attributes and mappings.
- Ensure the resource server client (`oauth2-playbook-resource-server`) has the appropriate audience and scope mappings so access tokens issued to the device client are accepted by the API.

## Create Resource Server from Scratch

### Skip below steps if you would like to continue with the existing resource server under `resource-server/src`

### Start with existing resource (Quick)

```shell
./device-code-flow/resource-server/setup-resource-server.sh
```

### Pre-requisite

```shell
brew install quarkusio/tap/quarkus

java -version
mvn -version
quarkus --version
```

### Create Resource Server Skeleton

```shell
cd ./device-code-flow

#Below command creates a quarkus project with required extensions
quarkus create app resource-server --extensions='resteasy,resteasy-jackson,quarkus-oidc' --no-code

cd resource-server
./mvnw quarkus:dev
```

### Enable https and OIDC configuration

Create TSL certificate and enable https in `application.properties` file.

```txt
    Static assets: src/main/resources/META-INF/resources/
    App configuration: src/main/resources/application.properties
    Code: src/main/java
```

```shell
cd src/main/resources

openssl req -x509 -newkey rsa:2048 \
  -keyout ./certs/key.pem \
  -out ./certs/cert.pem \
  -days 90 -nodes \
  -subj "/C=GB/ST=England/L=Manchester/O=example/OU=Technology/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,DNS:oauth2-playbook.dev,IP:127.0.0.1"
```

Update `application.properties` file with required properties, use the quarkus.io official documentation for reference.

- [Quarkus OIDC Guide](https://quarkus.io/guides/security-oidc-configuration-properties-reference)
- Add an example API resource class `UserResource.java` to handle user creation.
- Implement the JWT validation Login and scope checks using `@ScopesAllowed` annotation.

## Create ScopesAllowed Annotation

```java
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ScopesAllowed {
      String[] value();
  }
```

## Imlement a scopes validation filter 

```java
  public class AccessController implements ContainerRequestFilter {

    private static final Logger Logger = LoggerFactory.getLogger(AccessController.class);
    private final JsonWebToken jwt;
    private final ResourceInfo resourceInfo;

    public AccessController(ResourceInfo resourceInfo, JsonWebToken jwt) {
            this.resourceInfo = resourceInfo;
            this.jwt = jwt;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ScopesAllowed annotation = resourceInfo.getResourceMethod().getAnnotation(ScopesAllowed.class);
        if(Objects.isNull(annotation)){
            Logger.info("No ScopesAllowed annotation present, skipping scope validation");
            return;
        }
        String[] definedScopes = annotation.value();
        String jwtScopes = jwt.getClaim("scope");

        if (Objects.isNull(jwtScopes) || !hasRequiredScope(jwtScopes, definedScopes)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Forbidden: JWT does not have required scopes.")
                    .build());
            return;
        }
    }

    private boolean hasRequiredScope(String jwtScopes, String[] definedScopes) {
        List<String> definedScopesList = List.of(definedScopes);
        return definedScopesList.stream()
                .anyMatch(jwtScopes::contains);
    }
}
```

## Create a sample user using client script

Note: Once oauth2-provider and resource-server are up and running, you can use the client script under `device-code-flow/client/create-user.sh` to create a user using device code flow and call the resource server API.

the script uses curl commands to perform the device code flow steps and call the resource server.

- Device Authorization
- Polling Token Endpoint
- Create Resource - `POST - /api/user`
- GET Resource - `GET - /api/user/{id}`

```bash
cd ./device-code-flow/client

#testuser/testuser123
./create-user.sh 
```