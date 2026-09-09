package dev.javiercano.backendrescue.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/** Replace Hibernate SQL diagnostics before any appender sees raw database values. */
public class SqlDiagnosticFilter extends TurboFilter {
    private static final String SOURCE = "org.hibernate.engine.jdbc.spi.SqlExceptionHelper";

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format,
                              Object[] params, Throwable throwable) {
        if (!SOURCE.equals(logger.getName())) { return FilterReply.NEUTRAL; }
        // Hibernate's isEnabled checks also invoke this filter, with no message.
        if (format == null) { return FilterReply.NEUTRAL; }
        if (format != null && level.isGreaterOrEqual(Level.WARN)) {
            LoggerFactory.getLogger("dev.javiercano.backendrescue.database")
                    .warn("database_operation_failed diagnostic=redacted");
        }
        return FilterReply.DENY;
    }
}
