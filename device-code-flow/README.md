# Device Code Flow OAuth2 Example

## Architecture

![Device Code Flow Architecture](./device-code-flow-architecture.png)


## Start Keycloak Server (OAuth2 Provider)

```bash
./oauth2-provider/start.sh
```

## Resource Server (API Server)

### Pre-requisite

```shell
brew install quarkusio/tap/quarkus

java -version
mvn -version
quarkus --version
```

### Create Resource Server Skeleton

```shell
cd ./device-code-flow/resource-server

#Below command creates a quarkus project with required extensions
quarkus create app example-app --extensions='resteasy,resteasy-jackson,quarkus-oidc' --no-code

cd example-app
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
  -subj "/C=GB/ST=England/L=Manchester/O=finos/OU=Technology/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,DNS:oauth2-playbook.dev,IP:127.0.0.1"
```

Update `application.properties` file with required properties, use the quarkus.io official documentation for reference.

[Quarkus OIDC Guide](https://quarkus.io/guides/security-oidc-configuration-properties-reference)
