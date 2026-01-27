package com.espiedra.api.account.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * MAIN APPLICATION CLASS - Account Service
 * <p>
 * ARCHITECTURE:
 * - Hexagonal Architecture (Ports & Adapters)
 * - Domain-Driven Design (DDD)
 * - Event-Driven Architecture
 * - Reactive Programming (WebFlux + R2DBC)
 * <p>
 * COMMUNICATION:
 * - REST API: Port 8082
 * - Kafka Producer: banking.account.events, banking.movement.events
 * - Kafka Consumer: banking.customer.events
 * - WebClient: Customer Service (http://localhost:8081)
 * <p>
 * TECHNOLOGY STACK:
 * - Java 21
 * - Spring Boot 3.5.7
 * - Spring WebFlux (Reactive)
 * - Spring Data R2DBC
 * - PostgreSQL 16
 * - Apache Kafka
 * - Reactor Kafka
 * - Spring Security
 * - JWT (JJWT)
 * - MapStruct
 * - Lombok
 *
 * @author Emilio Piedra
 * @version 1.0.0
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class MainApplication {

	/**
	 * Main entry point
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

}
