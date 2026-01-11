# Fintech Project

The Trading project

## Prerequisites

- Java 17
- Maven

## Setup and Run

1.  Clone the repository.
2.  Navigate to the project directory.
3.  Run the application using Maven:
    ```sh
    ./mvnw spring-boot:run
    ```
4.  The application will be running on `http://localhost:8080`.

## Database

The project uses an in-memory H2 database. The H2 console is accessible at `http://localhost:8080/h2-console` with the following credentials:

-   **JDBC URL**: `jdbc:h2:mem:fintech`
-   **Username**: `sa`
-   **Password**: `password`

## API Document

The API documentation and Postman collection can be found in the project.

-   **Postman Collection**: `src/main/resources/docs/fintech-project-v1-api.json`

Import the Postman collection to test the API endpoints.
