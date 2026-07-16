package com.example.jfx.spring;

import javafx.application.Platform;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/navigation")
class NavigationRestController
{

    private final StageRouter router;

    NavigationRestController(StageRouter router)
    {
        this.router = router;
    }

    @PostMapping("/{view}")
    void navigate(@PathVariable String view)
    {
        // Scene mutations must happen on the JavaFX application thread, not the
        // servlet request thread this endpoint is invoked on.
        Platform.runLater(() -> router.navigateByUrl("/" + view));
    }
}
