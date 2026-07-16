package com.example.jfx.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class NavigationClient
{

    private final RestClient restClient;
    private final int serverPort;

    NavigationClient(RestClient.Builder restClientBuilder, @Value("${server.port}") int serverPort)
    {
        this.restClient = restClientBuilder.build();
        this.serverPort = serverPort;
    }

    void navigateTo(String view)
    {
        restClient.post()
                .uri("http://localhost:{port}/api/navigation/{view}", serverPort, view)
                .retrieve()
                .toBodilessEntity();
    }
}
