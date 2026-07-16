# JFX Spring Boot

## Description

Multi-module demo project showing how to integrate Spring Boot with JavaFX: bootstrapping a JavaFX app from Spring, injecting Spring beans into FXML controllers, and navigating between views using an Angular Router-inspired `StageRouter`.

- **`demo`** — the baseline app. Navigation between the primary/secondary stage happens in-process: an FXML controller calls `StageRouter.navigateByUrl(...)` directly, which runs on the JavaFX application thread already.
- **`demo-rest`** — the same app, but navigation goes over HTTP instead. The FXML controllers call a `NavigationClient` that POSTs to a `NavigationRestController` endpoint hosted by the app's own embedded servlet container; that endpoint then hops onto the JavaFX thread (`Platform.runLater`) and calls the same `StageRouter.navigateByUrl(...)`.

## Commands

Use the committed Maven wrapper (`mvnw` / `mvnw.cmd`) so everyone builds with the same Maven version, or `mvn` directly if you prefer your local install. Run from the repo root; use `-pl demo` or `-pl demo-rest` to target a single module:

- `.\mvnw.cmd compile` — compile both modules
- `.\mvnw.cmd -pl demo spring-boot:run` — run the baseline JavaFX app
- `.\mvnw.cmd -pl demo-rest spring-boot:run` — run the REST-navigation JavaFX app
- `.\mvnw.cmd test` — run all tests in both modules
- `.\mvnw.cmd -pl demo test -Dtest=MainApplicationTests` — run a single test class in one module

The root `pom.xml` is a `pom`-packaged parent aggregating the `demo` and `demo-rest` modules. Its `pluginManagement` declares `maven-compiler-plugin`'s `annotationProcessorPaths` (Lombok + `spring-boot-configuration-processor`) once for both modules — on newer JDKs, javac's implicit annotation-processor discovery via `-classpath` can silently skip Lombok (no error, `val`/`@Slf4j`/etc. just don't expand), so don't remove that config.

Each module has its own `jpackage.ps1` / `jpackage.sh` under a `scripts` folder, run from inside that folder:

- `demo\scripts\jpackage.ps1` / `demo/scripts/jpackage.sh` — builds `demo\target\dist\JfxSpringBoot\...`
- `demo-rest\scripts\jpackage.ps1` / `demo-rest/scripts/jpackage.sh` — builds `demo-rest\target\dist\JfxSpringBootRest\...`

Both invoke Maven via `..\mvnw.cmd` / `../mvnw` with `-pl <module>` so they build only that module out of the reactor, then assemble the plain app jar plus `mvn dependency:copy-dependencies` into a flat `target\jpackage-input` directory (jpackage's non-modular classpath only picks up jars directly in `--input`, not the Spring Boot fat jar's nested `BOOT-INF/` structure). Lombok is stripped from that directory afterward since it's compile-time only and unused at runtime. The Windows script produces both a no-console and a console launcher exe; the Linux script skips that distinction since Linux app-images don't have it.

## Architecture

Everything lives under a single package in each module: `com.example.jfx.spring`.

- **`MainApplication`** — Spring Boot entry point (`@SpringBootApplication`). `main()` doesn't call `SpringApplication.run`; it calls `Application.launch(JavaFxApplication.class, ...)` so the JavaFX toolkit drives the lifecycle instead.
- **`JavaFxApplication`** (extends `javafx.application.Application`) — bridges JavaFX and Spring:
  - `init()` builds a `SpringApplicationBuilder` and registers the JavaFX `Application`, `Parameters`, and `HostServices` as beans via an `ApplicationContextInitializer`, so they can be `@Autowired` elsewhere. In `demo` this is a headless context (`WebApplicationType.NONE`); in `demo-rest` it runs an embedded servlet container (`WebApplicationType.SERVLET`) so the app can host its navigation REST endpoint.
  - `start(primaryStage)` doesn't build UI directly — it publishes a `StageReadyEvent` (an inner `ApplicationEvent` wrapping the `Stage`) through the Spring context. This defers stage setup to a Spring-managed listener.
  - `stop()` closes the Spring context and calls `Platform.exit()`.
- **`PrimaryStageInitializer`** (`@Component`, package-private) — the `ApplicationListener<StageReadyEvent>` that actually builds the scene on startup, and also implements `StageRouter` to handle later navigation. Loads FXML via `FXMLLoader`, wiring `fxmlLoader.setControllerFactory(applicationContext::getBean)` so FXML controllers (`PrimaryController`, `SecondaryController`) are real Spring beans with constructor injection. Navigation (`navigateByUrl`) just re-resolves an FXML resource by name and swaps `scene.setRoot(...)`.
- **`StageRouter`** — one-method interface (`navigateByUrl(String url)`) implemented by `PrimaryStageInitializer`.
- **`PrimaryController` / `SecondaryController`** — `@Controller` beans bound to `primary.fxml` / `secondary.fxml` via `fx:controller`. Each has an `@FXML`-annotated handler that triggers navigation to the other view. In `demo` the handler calls `router.navigateByUrl(...)` directly. In `demo-rest` the handler calls `NavigationClient.navigateTo(...)` instead.
- **`AppProperties`** — `@ConfigurationProperties("app")` record backing the `app.*` keys in `application.properties` (title, index view, width, height). Requires `@ConfigurationPropertiesScan` on `MainApplication` (already present) rather than an explicit `@EnableConfigurationProperties`.
- FXML views live in `src/main/resources` (`primary.fxml`, `secondary.fxml`) and are looked up by name (e.g. `/primary` → `primary.fxml`) relative to `PrimaryStageInitializer`'s classloader.

`demo-rest` adds two classes on top of that shared shape:

- **`NavigationRestController`** — `@RestController` exposing `POST /api/navigation/{view}`. Hops onto the JavaFX application thread with `Platform.runLater` and calls the injected `StageRouter.navigateByUrl("/" + view)`.
- **`NavigationClient`** — `@Component` wrapping a `RestClient` that POSTs to `http://localhost:${server.port}/api/navigation/{view}`, i.e. the app calling back into its own embedded HTTP server. Injected into `PrimaryController` / `SecondaryController` in place of `StageRouter`.

## Advantages 

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

### When it's not worth it

If the app is single-user, local-only, doesn't need to share data or logic with other clients, and has no need for server-side security boundaries, a plain JavaFX app talking directly to a local DB (SQLite, embedded H2, etc.) is simpler and avoids the overhead of running/deploying/maintaining a separate backend service, network calls, latency, and two codebases instead of one. `demo-rest` illustrates the overhead side of that tradeoff concretely: navigating between two stages now involves an HTTP round-trip to the app's own embedded server instead of a direct method call.

The decision mostly comes down to: does anything else need to talk to this data/logic, or does it need to live somewhere other than the user's machine? If no, skip the backend.
