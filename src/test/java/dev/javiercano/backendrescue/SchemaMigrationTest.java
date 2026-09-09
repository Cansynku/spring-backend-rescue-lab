package dev.javiercano.backendrescue;

import dev.javiercano.backendrescue.config.LegacySchemaAdoption;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.assertj.core.api.Assertions.*;

class SchemaMigrationTest {
    private final String url = System.getProperty("spring.datasource.url", "jdbc:h2:mem:migration_tests;DB_CLOSE_DELAY=-1");
    private final String username = System.getProperty("spring.datasource.username", "sa");
    private final String password = System.getProperty("spring.datasource.password", "");
    private final boolean postgres = url.startsWith("jdbc:postgresql:");
    private final String schema = ("migration_test_" + UUID.randomUUID().toString().replace("-", ""))
            .transform(name -> postgres ? name : name.toUpperCase(Locale.ROOT));
    private DriverManagerDataSource datasource;

    @BeforeEach
    void setup() throws Exception {
        // Isolated schemas under the explicitly selected test database; no application tables are touched.
        datasource = new DriverManagerDataSource(url, username, password);
        try (var connection = datasource.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE SCHEMA " + schema);
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        try (var connection = datasource.getConnection(); var sql = connection.createStatement()) {
            sql.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private Flyway flyway() {
        return Flyway.configure().dataSource(datasource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration/" + (postgres ? "postgresql" : "h2"))
                .cleanDisabled(true).baselineOnMigrate(false).load();
    }

    private Connection connection() throws Exception {
        var connection = datasource.getConnection();
        try (var sql = connection.createStatement()) { sql.execute("SET SCHEMA '" + schema + "'"); }
        return connection;
    }

    private void legacy() throws Exception {
        Assumptions.assumeTrue(postgres, "Legacy schema adoption is PostgreSQL-only");
        try (var connection = connection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("legacy-baseline-postgresql.sql"));
        }
    }

    private List<String> snapshot() throws Exception {
        var snapshot = new ArrayList<String>();
        try (var connection = connection(); var sql = connection.createStatement()) {
            for (var query : List.of("SELECT id,customer_email,status,total_amount FROM purchase_orders ORDER BY id",
                    "SELECT id,amount,provider_payment_id,status,order_id FROM payments ORDER BY id")) {
                try (var rows = sql.executeQuery(query)) {
                    while (rows.next()) {
                        var values = new ArrayList<String>();
                        for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++) {
                            values.add(String.valueOf(rows.getObject(column)));
                        }
                        snapshot.add(String.join("|", values));
                    }
                }
            }
        }
        return snapshot;
    }

    @Test
    void createsEmptySchemaAndSecondRunDoesNothing() throws Exception {
        var flyway = flyway();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
        try (var connection = connection(); var sql = connection.createStatement()) {
            sql.executeQuery("SELECT idempotency_key FROM payments").close();
        }
    }

    @Test
    void rejectsUnmanagedSchemaWithoutExplicitAdoption() throws Exception {
        try (var connection = connection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE unrelated (id INTEGER)");
        }
        assertThatThrownBy(() -> flyway().migrate()).hasMessageContaining("non-empty schema");
    }

    @Test
    void adoptsBaselinePreservesAllValuesAndUpdatesPaymentConstraints() throws Exception {
        legacy();
        var before = snapshot();
        var flyway = flyway();
        LegacySchemaAdoption.adoptAndMigrate(flyway, "baseline-v1");
        assertThat(snapshot()).isEqualTo(before);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try (var connection = connection(); var sql = connection.createStatement()) {
            try (var rows = sql.executeQuery("SELECT count(*) FROM payments WHERE idempotency_key IS NULL")) {
                rows.next(); assertThat(rows.getInt(1)).isEqualTo(2);
            }
            sql.executeUpdate("UPDATE payments SET status='PENDING',idempotency_key='migration-key' WHERE provider_payment_id='sandbox-legacy'");
            sql.executeUpdate("UPDATE payments SET status='UNKNOWN' WHERE provider_payment_id='sandbox-legacy'");
            assertThatThrownBy(() -> sql.executeUpdate("UPDATE payments SET idempotency_key='migration-key' WHERE provider_payment_id IS NULL"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void rejectsWrongShapeWithoutCreatingHistoryOrChangingData() throws Exception {
        legacy();
        var before = snapshot();
        try (var connection = connection(); var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE payments ADD COLUMN unexpected INTEGER");
        }
        assertThatThrownBy(() -> LegacySchemaAdoption.adoptAndMigrate(flyway(), "baseline-v1"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("column");
        assertThat(snapshot()).isEqualTo(before);
        try (var connection = connection(); var sql = connection.createStatement();
             var rows = sql.executeQuery("SELECT to_regclass('" + schema + ".flyway_schema_history')")) {
            rows.next(); assertThat(rows.getString(1)).isNull();
        }
    }

    @Test
    void rejectsWeakenedStatusConstraintWithoutAdopting() throws Exception {
        legacy();
        try (var connection = connection(); var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE payments DROP CONSTRAINT payments_status_check");
        }
        assertThatThrownBy(() -> LegacySchemaAdoption.adoptAndMigrate(flyway(), "baseline-v1"))
                .hasMessageContaining("constraints");
    }

    @Test
    void adoptsExistingPaymentSchemaWithoutChangingRowsOrAddingDuplicateColumn() throws Exception {
        legacy();
        try (var connection = connection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/postgresql/V2__payment_intent.sql"));
            try (var sql = connection.createStatement()) {
                sql.executeUpdate("UPDATE payments SET idempotency_key='existing-key',status='UNKNOWN' WHERE provider_payment_id IS NULL");
            }
        }
        var before = snapshot();
        var flyway = flyway();
        LegacySchemaAdoption.adoptAndMigrate(flyway, "payment-v2");
        assertThat(snapshot()).isEqualTo(before);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try (var connection = connection(); var sql = connection.createStatement();
             var rows = sql.executeQuery("SELECT idempotency_key FROM payments WHERE status='UNKNOWN'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("existing-key");
        }
        assertThatThrownBy(() -> LegacySchemaAdoption.adoptAndMigrate(flyway, "payment-v2"))
                .hasMessageContaining("already has Flyway history");
    }

    @Test
    void detectsChangedAppliedMigration() throws Exception {
        var flyway = flyway();
        flyway.migrate();
        try (var connection = connection(); var sql = connection.createStatement()) {
            sql.executeUpdate("UPDATE \"flyway_schema_history\" SET \"checksum\"=0 WHERE \"version\"='2'");
        }
        assertThatThrownBy(flyway::validate).hasMessageContaining("checksum");
    }
}
