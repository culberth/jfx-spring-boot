package com.example.jfx.spring;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MainApplication
{

    public static void main(String[] args)
    {
        Application.launch(JavaFxApplication.class, args);
    }

}
