package com.example.jfx.spring;

import javafx.fxml.FXML;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SecondaryController
{

    private final NavigationClient navigationClient;

    @FXML
    private void switchToPrimary()
    {
        navigationClient.navigateTo("primary");
    }
}
