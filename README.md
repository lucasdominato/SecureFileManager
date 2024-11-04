# Thought Process notes

GET File by ID including username into the query to avoid unnecessary queries in the database
This might be less flexible for future changes but increase the performance because there is a index for username

Chose to use structured fields for metadata instead of key-value to avoid complex queries and type validation

I've considered to use an external storage such as Amazon S3 to store the file binaries, but the requirements asked to persiste data in database.

To use database to store file binaries my suggestion would be to use separate metadata from binaries in two different tables.
Using this strategy we can improve index and performance of the metadata.

For the binaries we can use a couple of techniques such as TOAST, partitioning and even multi disk for binaries tablespace (improving I/O concurrency for data-only tables and binaries table).

The only issue found is when downloading the streaming is not working properly and it is loading the file into memory.

# SecureFileManager

## Prerequisites

To run this project locally, you need:

- **Java 17** installed
- **Gradle** for project build and dependencies management
- **Docker and Docker Compose** for setting up PostgreSQL

## Setting up the Database

The project includes a Docker Compose configuration file to quickly start a local PostgreSQL instance.

Navigate to the root folder of this project and then run the below command.

To start the database:

```
docker compose -f docker-compose.yml up -d
```

This will create a PostgreSQL instance with the necessary configuration for development.

## Configuring Secrets

The application requires certain secrets for encryption. You can generate these secrets with `openssl` as follows:

```
openssl rand -base64 32
```

Set these generated secrets in `application-local.properties` under `src/main/resources`:

```
jwt.secret-key=<YOUR_GENERATED_JWT_SECRET_KEY>
encryption.aes-key=<YOUR_GENERATED_AES_KEY>
encryption.hmac-key=<YOUR_GENERATED_HMAC_KEY>
```

Replace `<YOUR_GENERATED_JWT_SECRET_KEY>`, `<YOUR_GENERATED_AES_KEY>` and `<YOUR_GENERATED_HMAC_KEY>` with the generated secrets.

## Running the Application

Once the database is set up and secrets are configured, you can start the application in local development mode using:

```
gradle bootRun --args='--spring.profiles.active=local'
```

The application will be available at `http://localhost:8080`.

## Endpoints Documentation

API documentation is available through Swagger once the application is running. You can access it at `http://localhost:8080/swagger-ui.html`.

---

Enjoy using SecureFileManager!
