package com.example.jfx.spring;

import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class PrimaryController
{

    private final String appMessage;

    private final NavigationClient navigationClient;

    public PrimaryController(@Value("${app.message}") String appMessage, NavigationClient navigationClient)
    {
        this.appMessage = appMessage;
        this.navigationClient = navigationClient;
    }

    @FXML
    private void switchToSecondary()
    {
        log.info(appMessage);
        navigationClient.navigateTo("secondary");
    }
}
