package dev.javiercano.backendrescue;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class StandaloneStorageTest {
    @TempDir Path directory;

    @Test void fileDatabaseRetainsOrdersAfterClosingAndReopening() throws Exception {
        var url = "jdbc:h2:file:" + directory.resolve("orders").toAbsolutePath().toString().replace('\\', '/')
                + ";DB_CLOSE_ON_EXIT=FALSE";
        var flyway = Flyway.configure().dataSource(url, "sa", "")
                .locations("classpath:db/migration/h2").cleanDisabled(true).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO purchase_orders(id,customer_email,status,total_amount) "
                    + "VALUES(RANDOM_UUID(),'demo@example.com','CREATED',25.50)");
        }
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT total_amount FROM purchase_orders")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getBigDecimal(1)).isEqualByComparingTo("25.50");
            assertThat(rows.next()).isFalse();
        }
    }
}
