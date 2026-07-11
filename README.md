# JFX Playground

## Description

Demo project for integrating _Spring Boot_ and _JavaFX_. The Demo has the following features:

1. Simple working sample code for a small desktop application.
2. Configuration for _maven_ or _gradle_ project to use _JavaFX_ with _Spring Boot_, with the appropriate maven artifact.
   dependencies for basic development.
3. Uses recent, reasonably up-to-date _JavaFX_ modules, with consistent versions.
4. Demonstrates placing an _FXML_ file in a resource directory, and looking it up as a resource at runtime.
5. Properly separates a controller class from the application class.
6. Demonstrates how to bootstrap _JavaFX_ application using _Spring Boot_.
7. Demonstrates how to use _Spring Boot_ features within _JavaFX_.
8. Showcase how to navigate between _JavaFX_ view using a router implementation, heavily inspired on Angular/Router.

## Getting Started

### Advantages 

**Architecture / maintainability**
- Business logic, validation, and data access live server-side, decoupled from UI code. You can change the backend without touching the client, or swap the UI framework entirely later.
- Easier to test business logic in isolation (Spring Boot's testing tools) vs. testing logic tangled into JavaFX controllers.

**Multi-client support**
- The Spring Boot backend can serve other clients too — a web app, mobile app, CLI, other desktop installs — since it exposes REST/GraphQL APIs rather than logic baked into one JavaFX binary.

**Centralized/shared data**
- If multiple users or machines need to see the same data, a backend with a real database (rather than each desktop app owning local state) gives you a single source of truth, concurrent access control, and easier syncing.

**Security**
- Sensitive logic, credentials, and direct database access stay on the server, not shipped inside a distributable desktop binary a user could decompile or inspect.

**Scalability & offloading work**
- Heavy computation, batch jobs, or large data processing can run on a server rather than the user's machine, which matters if the client hardware is constrained.

**Independent deployment/updates**
- You can patch backend bugs or roll out logic changes without pushing a new desktop app release. Client only needs updates for UI changes.

**Ecosystem/integration**
- Spring Boot makes it easy to plug into things like Spring Security, Spring Data, message queues, scheduling, external APIs — infrastructure that's awkward to replicate in a plain desktop app talking directly to a database.

### When it's not worth it**

If the app is single-user, local-only, doesn't need to share data or logic with other clients, and has no need for server-side security boundaries, a plain JavaFX app talking directly to a local DB (SQLite, embedded H2, etc.) is simpler and avoids the overhead of running/deploying/maintaining a separate backend service, network calls, latency, and two codebases instead of one.

The decision mostly comes down to: does anything else need to talk to this data/logic, or does it need to live somewhere other than the user's machine? If no, skip the backend.
