package com.bvcgroup.vlearn.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bvcgroup.vlearn.support.PostgresIntegrationTest;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BaselineMigrationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    Flyway flyway;

    @Autowired
    DataSource dataSource;

    @Test
    void migrate_emptyPostgresDatabase_appliesBaseline() throws SQLException {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }
}
