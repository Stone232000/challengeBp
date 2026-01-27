package com.espiedra.api.account.service.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * INFRASTRUCTURE CONFIGURATION: WebClientConfig
 * <p>
 * DESIGN PATTERNS APPLIED:
 * - Factory Pattern: Creates and configures WebClient bean
 * - Configuration Pattern: Centralized WebClient configuration
 * - Dependency Injection: Bean injected into HTTP client adapters
 * - Reactive Streams: Non-blocking I/O with Reactor Netty
 * - Circuit Breaker Pattern: Timeout for fail-fast behavior
 * <p>
 * SOLID PRINCIPLES:
 * - Single Responsibility: Only handles WebClient configuration
 * - Dependency Inversion: Adapters depend on abstraction (WebClient)
 * - Open/Closed: Open for new configurations, closed for modification
 * <p>
 * WEBCLIENT OVERVIEW:
 * ===================
 * Spring WebClient is a reactive HTTP client for non-blocking I/O:
 * <p>
 * ADVANTAGES:
 * - Reactive Streams: Non-blocking requests (efficient resource usage)
 * - Backpressure: Handles slow responses without blocking threads
 * - Composable: Integrates with Reactor operators (map, flatMap, retry)
 * - Scalable: Handles thousands of concurrent connections
 * - Thread-efficient: No thread-per-connection
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * Customer Service base URL
     */
    @Value("${app.customer-service.base-url}")
    private String customerServiceBaseUrl;

    /**
     * Create WebClient bean for Customer Service communication
     * @return WebClient Configured WebClient for Customer Service
     */
    @Bean
    public WebClient webClient() {
        log.info("Configuring WebClient for Customer Service: baseUrl={}", customerServiceBaseUrl);

        // Configure Netty HTTP client with timeouts and connection pool
        HttpClient httpClient = HttpClient.create()
            // CONNECTION TIMEOUT: Max time to establish TCP connection
            // Fails fast if service is unreachable
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            // RESPONSE TIMEOUT: Max time for entire request-response cycle
            // Includes connection + read + write
            .responseTimeout(Duration.ofSeconds(5))
            // CHANNEL PIPELINE: Add read/write timeout handlers
            .doOnConnected(connection -> {
                // READ TIMEOUT: Max time to read response after request sent
                // Fails fast if service is slow/hung
                connection.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS));
                // WRITE TIMEOUT: Max time to write request to socket
                // Rare, but prevents hanging on slow networks
                connection.addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS));
            });

        // Build WebClient with Reactor Netty HTTP client
        WebClient client = WebClient.builder()
            // BASE URL: All requests relative to this URL
            .baseUrl(customerServiceBaseUrl)
            // HTTP CLIENT: Use Reactor Netty with timeouts
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            // Build WebClient instance
            .build();

        log.info("WebClient configured successfully: " +
            "baseUrl={}, connectionTimeout=5s, readTimeout=5s, responseTimeout=5s",
            customerServiceBaseUrl);

        return client;
    }
}
