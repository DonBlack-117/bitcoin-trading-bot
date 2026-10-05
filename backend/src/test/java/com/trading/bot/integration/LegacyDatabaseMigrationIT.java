package com.trading.bot.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduce la base que ya existe en la máquina (tablas de Hibernate sin historial de Flyway)
 * y comprueba que V2 convierte las columnas sin perder las filas.
 */
class LegacyDatabaseMigrationIT extends MariaDbContainerSupport {

    @Test
    void baselinesTheExistingSchemaAndKeepsItsRows() throws Exception {
        DriverManagerDataSource admin = new DriverManagerDataSource(
                MARIADB.getJdbcUrl().replace("/tradingbot", "/"), "root", MARIADB.getPassword());
        new JdbcTemplate(admin).execute("CREATE DATABASE legacy");
        new JdbcTemplate(admin).execute("GRANT ALL ON legacy.* TO 'bot'@'%'");

        String url = mysqlUrl().replace("/tradingbot", "/legacy");
        DriverManagerDataSource ds = new DriverManagerDataSource(url, "bot", "bot");
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // Estado anterior: el esquema de V1 creado a mano y los datos reales del 13 de mayo
        try (Connection c = ds.getConnection()) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/migration/V1__init.sql"));
        }
        jdbc.update("INSERT INTO portfolio_snapshots (btc_balance, btc_price, mxn_balance, snapshot_at, total_value_mxn, unrealized_pnl) "
                + "VALUES (0, 1398490, 50000, '2026-05-13 02:51:04.873954', 50000, 0)");
        jdbc.update("INSERT INTO signals_history (confianza, precio, score_buy, score_sell, senal, strategy_breakdown, timestamp) "
                + "VALUES (56, 1398490.123, 6, 4, 'COMPRAR', '{}', '2026-05-13 02:51:05.500253')");

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
        var result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");

        Map<String, Object> snapshot = jdbc.queryForMap("SELECT * FROM portfolio_snapshots");
        assertThat((BigDecimal) snapshot.get("mxn_balance")).isEqualByComparingTo("50000.00");
        assertThat((BigDecimal) snapshot.get("btc_price")).isEqualByComparingTo("1398490.00");

        Map<String, Object> signal = jdbc.queryForMap("SELECT * FROM signals_history");
        assertThat((BigDecimal) signal.get("precio")).isEqualByComparingTo("1398490.12");
        assertThat(signal.get("senal")).isEqualTo("COMPRAR");
    }
}
