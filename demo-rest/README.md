# demo-rest

Same app as [`demo`](../demo), but switching between the primary and secondary stage goes over HTTP instead of a direct method call. This module exists to show what that costs relative to `demo`'s in-process `StageRouter.navigateByUrl(...)` call — see the root [README.md](../README.md) for the general project description and commands.

## What's different from `demo`

- **`pom.xml`** — depends on `spring-boot-starter-web` instead of the plain `spring-boot-starter`, so an embedded servlet container (Tomcat) is on the classpath. Artifact id is `jfx-spring-boot-rest` instead of `jfx-spring-boot`.
- **[`JavaFxApplication.java`](src/main/java/com/example/jfx/spring/JavaFxApplication.java)** — boots the `SpringApplicationBuilder` with `.web(WebApplicationType.SERVLET)` instead of `.web(WebApplicationType.NONE)`, so Spring actually starts that embedded server.
- **[`NavigationRestController.java`](src/main/java/com/example/jfx/spring/NavigationRestController.java)** — new class, not present in `demo`. A `@RestController` exposing `POST /api/navigation/{view}`. Since it runs on a servlet request thread and JavaFX forbids scene mutation off the FX application thread, it hops over with `Platform.runLater(...)` before calling `StageRouter.navigateByUrl("/" + view)` — the same `StageRouter` that `demo`'s controllers call directly.
- **[`NavigationClient.java`](src/main/java/com/example/jfx/spring/NavigationClient.java)** — new class, not present in `demo`. A `@Component` wrapping a `RestClient` that POSTs to `http://localhost:${server.port}/api/navigation/{view}`, i.e. the app calling back into its own embedded HTTP server.
- **[`PrimaryController.java`](src/main/java/com/example/jfx/spring/PrimaryController.java) / [`SecondaryController.java`](src/main/java/com/example/jfx/spring/SecondaryController.java)** — take a `NavigationClient` constructor dependency instead of `StageRouter`, and their `@FXML` handlers call `navigationClient.navigateTo("secondary" | "primary")` instead of `router.navigateByUrl(...)`. `StageRouter` itself, `PrimaryStageInitializer`, the FXML files, and `AppProperties` are all unchanged from `demo`.
- **`application.properties`** — adds `server.port=8080` so `NavigationClient` knows what port to call without extra plumbing (e.g. capturing a `WebServerInitializedEvent` for a random port). `app.title` is changed to `JFX Application (REST navigation)` to make the two apps distinguishable when both are running.
- **`scripts/jpackage.ps1` / `scripts/jpackage.sh`** — same approach as `demo`'s, just pointed at this module (`-pl demo-rest`) and using this module's jar/app names (`jfx-spring-boot-rest-0.0.1-SNAPSHOT.jar`, `JfxSpringBootRest`).

## REST API

One endpoint, served by the app's own embedded Tomcat on `server.port` (`8080` by default):

| Method | Path                  | Path variable                  | Request body | Response         |
|--------|-----------------------|---------------------------------|---------------|-------------------|
| `POST` | `/api/navigation/{view}` | `view` — `primary` or `secondary` (any FXML resource name under `src/main/resources` actually works, since it's passed straight to `StageRouter.navigateByUrl("/" + view)`) | none | `200 OK`, empty body |

It's implemented by [`NavigationRestController`](src/main/java/com/example/jfx/spring/NavigationRestController.java) and is only ever called by this same app, via [`NavigationClient`](src/main/java/com/example/jfx/spring/NavigationClient.java) — it isn't meant to be a public API, just the mechanism the demo uses to move navigation out of the JavaFX process and onto a REST call. Since it's plain Spring MVC with no auth, security config, or CORS handling, anything on localhost can also call it directly, e.g.:

```
curl -X POST http://localhost:8080/api/navigation/secondary
```

## Request flow

1. User clicks the button in `primary.fxml`.
2. `PrimaryController.switchToSecondary()` calls `navigationClient.navigateTo("secondary")`.
3. `NavigationClient` sends `POST http://localhost:8080/api/navigation/secondary` to the app's own embedded Tomcat.
4. `NavigationRestController.navigate("secondary")` runs on a servlet thread, and schedules `router.navigateByUrl("/secondary")` via `Platform.runLater`.
5. `PrimaryStageInitializer` (as `StageRouter`) loads `secondary.fxml` and swaps it into the existing `Scene` on the JavaFX application thread — exactly like it does in `demo`.
