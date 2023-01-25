package dev.ikwattro;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
public class Neo4j5RestoreBackupExampleTest {

    @Container
    private Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5.3.0-enterprise")
            .withAdminPassword("password")
            .withEnv("NEO4J_dbms_memory_heap_max__size", "256M")
            .withEnv("NEO4J_dbms_databases_seed__from__uri__providers", "URLConnectionSeedProvider")
            .withClasspathResourceMapping("backups", "/backups", BindMode.READ_ONLY)
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");


    @BeforeAll
    void beforeAll() {
        neo4j.start();
        createDbFromBackup();
    }
    @Test
    void testCreatingDbFromBackup() {
        try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", "password"))) {
            try (Session session = driver.session(SessionConfig.forDatabase("worldcup22"))) {
                var result = session.run("MATCH (n) RETURN count(n) AS c").single().get("c").asLong();
                assertThat(result).isPositive();
            }
        }
    }

    private void createDbFromBackup() {
        try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.basic("neo4j", "password"))) {
            try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
                session.run("""
                        CREATE DATABASE worldcup22 OPTIONS { existingData: "use", seedUri: "file:///backups/world-cup-2022-neo4j.backup"}
                        """);
            }
        }
    }

}
