# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Use the committed Maven wrapper (`mvnw` / `mvnw.cmd`) so everyone builds with the same Maven version, or `mvn` directly if you prefer your local install:

- `.\mvnw.cmd compile` — compile
- `.\mvnw.cmd spring-boot:run` — run the JavaFX app
- `.\mvnw.cmd test` — run all tests
- `.\mvnw.cmd test -Dtest=MainApplicationTests` — run a single test class

The `maven-compiler-plugin` explicitly declares `annotationProcessorPaths` (Lombok + `spring-boot-configuration-processor`) — on newer JDKs, javac's implicit annotation-processor discovery via `-classpath` can silently skip Lombok (no error, `val`/`@Slf4j`/etc. just don't expand), so don't remove that config.

`.\jpackage.ps1` (Windows/PowerShell) builds a self-contained native executable at `target\dist\JfxPlayground\JfxPlayground.exe` via `jpackage` (`--type app-image`, bundles its own JRE, no installer/WiX needed). It works by building the plain (pre-repackage) app jar plus `mvn dependency:copy-dependencies` into a flat `target\jpackage-input` directory, since jpackage's non-modular classpath only picks up jars directly in `--input`, not the Spring Boot fat jar's nested `BOOT-INF/` structure. Lombok is stripped from that directory afterward since it's compile-time only and unused at runtime.

`./jpackage.sh` (Linux/bash) is the equivalent for Linux builds, producing `target/dist/JfxPlayground/bin/JfxPlayground`. Same jpackage-input assembly approach; it skips the Windows-only console/no-console dual-launcher step since Linux app-images don't have that distinction.

## Architecture

Everything lives under a single package: `com.example.jfx.spring`.

- **`MainApplication`** — Spring Boot entry point (`@SpringBootApplication`). `main()` doesn't call `SpringApplication.run`; it calls `Application.launch(JavaFxApplication.class, ...)` so the JavaFX toolkit drives the lifecycle instead.
- **`JavaFxApplication`** (extends `javafx.application.Application`) — bridges JavaFX and Spring:
  - `init()` builds a headless (`WebApplicationType.NONE`) `SpringApplicationBuilder` and registers the JavaFX `Application`, `Parameters`, and `HostServices` as beans via an `ApplicationContextInitializer`, so they can be `@Autowired` elsewhere.
  - `start(primaryStage)` doesn't build UI directly — it publishes a `StageReadyEvent` (an inner `ApplicationEvent` wrapping the `Stage`) through the Spring context. This defers stage setup to a Spring-managed listener.
  - `stop()` closes the Spring context and calls `Platform.exit()`.
- **`PrimaryStageInitializer`** (`@Component`, package-private) — the `ApplicationListener<StageReadyEvent>` that actually builds the scene on startup, and also implements `StageRouter` to handle later navigation. Loads FXML via `FXMLLoader`, wiring `fxmlLoader.setControllerFactory(applicationContext::getBean)` so FXML controllers (`PrimaryController`, `SecondaryController`) are real Spring beans with constructor injection. Navigation (`navigateByUrl`) just re-resolves an FXML resource by name and swaps `scene.setRoot(...)`.
- **`StageRouter`** — one-method interface (`navigateByUrl(String url)`) implemented by `PrimaryStageInitializer`; injected into controllers so they aren't coupled to stage/scene mechanics.
- **`PrimaryController` / `SecondaryController`** — `@Controller` beans bound to `primary.fxml` / `secondary.fxml` via `fx:controller`. Each has an `@FXML`-annotated handler that calls `router.navigateByUrl(...)` to flip to the other view.
- **`AppProperties`** — `@ConfigurationProperties("app")` record backing the `app.*` keys in `application.properties` (title, index view, width, height). Requires `@ConfigurationPropertiesScan` on `MainApplication` (already present) rather than an explicit `@EnableConfigurationProperties`.
- FXML views live in `src/main/resources` (`primary.fxml`, `secondary.fxml`) and are looked up by name (e.g. `/primary` → `primary.fxml`) relative to `PrimaryStageInitializer`'s classloader.

When adding a new view: create the FXML in `src/main/resources`, add an `@Controller` class with `fx:controller` wired to it, inject `StageRouter` if it needs to navigate, and reference the view by its `/name` (no `.fxml` extension) when calling `navigateByUrl` or setting `app.index-view`.
