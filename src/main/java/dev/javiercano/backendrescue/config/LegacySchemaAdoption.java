package dev.javiercano.backendrescue.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Explicit opt-in for the two observed Hibernate schemas; never enables automatic baselining. */
@Configuration
@ConditionalOnProperty(name = "lab.schema-adoption")
public class LegacySchemaAdoption {
    @Bean
    FlywayMigrationStrategy adoptKnownSchema(@Value("${lab.schema-adoption}") String mode) {
        return flyway -> adoptAndMigrate(flyway, mode);
    }

    public static void adoptAndMigrate(Flyway flyway, String mode) {
        int version = switch (mode) {
            case "baseline-v1" -> 1;
            case "payment-v2" -> 2;
            default -> throw new IllegalArgumentException("Unknown schema adoption mode");
        };
        String schema = Optional.ofNullable(flyway.getConfiguration().getDefaultSchema()).orElse("public");
        try (var connection = flyway.getConfiguration().getDataSource().getConnection()) {
            if (!connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")) {
                throw new IllegalStateException("Legacy adoption is supported only for the observed PostgreSQL schemas");
            }
            var tables = values(connection,
                    "select table_name from information_schema.tables where table_schema = ?", schema);
            if (tables.contains("flyway_schema_history")) {
                throw new IllegalStateException("Schema already has Flyway history; remove lab.schema-adoption and start normally");
            }
            if (!tables.equals(Set.of("payments", "purchase_orders"))) {
                throw new IllegalStateException("Legacy schema must contain exactly the two known tables");
            }
            verifyColumns(connection, schema, version);
            verifyConstraints(connection, schema, version);
        } catch (SQLException error) {
            throw new IllegalStateException("Cannot verify legacy schema; no adoption was performed", error);
        }
        // Shape checked before creating any history. Call only during a maintenance window.
        var adoption = Flyway.configure().configuration(flyway.getConfiguration())
                .baselineVersion(Integer.toString(version)).baselineDescription("Verified legacy " + mode).load();
        adoption.baseline();
        adoption.migrate();
    }

    private static void verifyColumns(Connection connection, String schema, int version) throws SQLException {
        Set<String> expected = new HashSet<>(Set.of(
                "purchase_orders.id:uuid:0:0:0:NO", "purchase_orders.customer_email:character varying:255:0:0:YES",
                "purchase_orders.status:character varying:255:0:0:YES", "purchase_orders.total_amount:numeric:0:38:2:YES",
                "payments.id:uuid:0:0:0:NO", "payments.amount:numeric:0:38:2:YES",
                "payments.provider_payment_id:character varying:255:0:0:YES", "payments.status:character varying:255:0:0:YES",
                "payments.order_id:uuid:0:0:0:NO"));
        if (version == 2) { expected.add("payments.idempotency_key:character varying:128:0:0:YES"); }
        Set<String> actual = new HashSet<>();
        try (var statement = connection.prepareStatement("""
                select table_name, column_name, data_type, character_maximum_length,
                       numeric_precision, numeric_scale, is_nullable, column_default, is_identity, is_generated
                from information_schema.columns where table_schema = ?
                """)) {
            statement.setString(1, schema);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    if (rows.getString(8) != null || !rows.getString(9).equals("NO") || !rows.getString(10).equals("NEVER")) {
                        throw new IllegalStateException("Unexpected column default or generated value in legacy schema");
                    }
                    actual.add(rows.getString(1) + "." + rows.getString(2) + ":" + rows.getString(3) + ":"
                            + rows.getInt(4) + ":" + rows.getInt(5) + ":" + rows.getInt(6) + ":" + rows.getString(7));
                }
            }
        }
        if (!actual.equals(expected)) { throw new IllegalStateException("Legacy column definitions differ from the supported snapshot"); }
    }

    private static void verifyConstraints(Connection connection, String schema, int version) throws SQLException {
        // Compare definitions, not Hibernate's generated FK/unique names. V2 changes the known status constraint.
        Set<String> expected = new HashSet<>(Set.of(
                "purchase_orders:p:PRIMARY KEY (id)", "payments:p:PRIMARY KEY (id)",
                "payments:f:FOREIGN KEY (order_id) REFERENCES purchase_orders(id)",
                "purchase_orders:c:CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'PAID'::character varying, 'CANCELLED'::character varying])::text[])))",
                version == 1
                    ? "payments:c:CHECK (((status)::text = ANY ((ARRAY['AUTHORIZED'::character varying, 'FAILED'::character varying])::text[])))"
                    : "payments:c:CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'AUTHORIZED'::character varying, 'FAILED'::character varying, 'UNKNOWN'::character varying])::text[])))"));
        if (version == 2) { expected.add("payments:u:UNIQUE (idempotency_key)"); }
        try (var statement = connection.prepareStatement("select set_config('search_path', quote_ident(?), false)")) {
            statement.setString(1, schema);
            statement.execute();
        }
        var actual = values(connection, """
                select t.relname || ':' || c.contype::text || ':' || pg_get_constraintdef(c.oid)
                from pg_constraint c join pg_class t on t.oid=c.conrelid
                join pg_namespace n on n.oid=t.relnamespace where n.nspname = ?
                """, schema);
        if (!actual.equals(expected)) { throw new IllegalStateException("Legacy constraints differ from the supported snapshot"); }
        if (!values(connection, """
                select c.conname from pg_constraint c join pg_class t on t.oid=c.conrelid
                join pg_namespace n on n.oid=t.relnamespace
                where n.nspname = ? and (not c.convalidated or c.condeferrable or c.condeferred)
                """, schema).isEmpty()) {
            throw new IllegalStateException("Legacy constraints must be validated and immediate");
        }
        var statusNames = values(connection, """
                select c.conname from pg_constraint c join pg_class t on t.oid=c.conrelid
                join pg_namespace n on n.oid=t.relnamespace
                where n.nspname = ? and t.relname='payments' and c.contype='c'
                """, schema);
        if (!statusNames.equals(Set.of("payments_status_check"))) {
            throw new IllegalStateException("Unexpected payment status constraint name");
        }
        if (!values(connection, "select trigger_name from information_schema.triggers where trigger_schema = ?", schema).isEmpty()) {
            throw new IllegalStateException("Legacy schema has unexpected triggers");
        }
    }

    private static Set<String> values(Connection connection, String sql, String schema) throws SQLException {
        var values = new HashSet<String>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (var rows = statement.executeQuery()) { while (rows.next()) { values.add(rows.getString(1)); } }
        }
        return values;
    }
}
