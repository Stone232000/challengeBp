package com.espiedra.api.customer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * API Customer Service Application
 * <p>
 * Spring Boot application for Customer microservice.
 * <p>
 * ARCHITECTURE:
 * - Hexagonal Architecture (Ports & Adapters)
 * - Event-Driven Architecture (Kafka)
 * - Reactive Programming (WebFlux, R2DBC)
 * <p>
 * TECHNOLOGIES:
 * - Spring Boot 3.x
 * - Spring WebFlux (Reactive)
 * - Spring Data R2DBC (Reactive Database)
 * - Spring Security (JWT)
 * - Reactor Kafka (Event Publishing)
 * - MapStruct (Object Mapping)
 * - OpenAPI Code Generation
 *
 * @version 1.0.1
 */
@Slf4j
@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "com.espiedra.api.customer.service.infrastructure.adapter.output.persistence.repository")
public class MainApplication {

	public static void main(String[] args) {
		log.info("Starting Customer Service Application...");
		SpringApplication.run(MainApplication.class, args);
		log.info("Customer Service Application started successfully!");
	}

}
