package com.lucasdominato.securefilemanager;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = SecureFileManagerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"server.port=0",
		"local.management.port=0",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.show-sql=true",
		"jwt.secret-key=nL4qbOMfunmPIcGwiTxXKphaf9TeAXtVqqckslGhqT0=",
		"encryption.aes-key=72nwGtTEtKp7Ye+oXg1aQcEvWZDvFG9a2hho4RLP76Q=",
		"encryption.hmac-key=ehGRZTRXWXm00/2GafSlIpJbxotDjQK2gD7y2Uyy4+Q="
})
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractIntegrationTest {

	@LocalServerPort
	protected int localServerPort;

	@LocalManagementPort
	protected int localServerManagementPort;

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
			.withDatabaseName("securefilemanager");

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("securefilemanager.database.host", postgres::getHost);
		registry.add("securefilemanager.database.port", () -> postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
		registry.add("securefilemanager.database.name", postgres::getDatabaseName);
		registry.add("securefilemanager.database.username", postgres::getUsername);
		registry.add("securefilemanager.database.password", postgres::getPassword);
	}
}