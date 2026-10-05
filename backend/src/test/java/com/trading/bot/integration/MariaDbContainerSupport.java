package com.trading.bot.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * MariaDB 11.8 real en un contenedor (Podman o Docker), la misma versión que la base local.
 * La app se conecta con el driver de MySQL, igual que en producción.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class MariaDbContainerSupport {

    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(
            DockerImageName.parse("docker.io/library/mariadb:11.8").asCompatibleSubstituteFor("mariadb"))
            .withDatabaseName("tradingbot")
            .withUsername("bot")
            .withPassword("bot");

    static {
        MARIADB.start();
    }

    static String mysqlUrl() {
        return "jdbc:mysql://" + MARIADB.getHost() + ":" + MARIADB.getMappedPort(3306) + "/tradingbot";
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MariaDbContainerSupport::mysqlUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("bot.scheduler.enabled", () -> "false");
        registry.add("cmc.api.key", () -> "");
    }
}
