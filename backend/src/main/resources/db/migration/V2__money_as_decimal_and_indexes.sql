-- Montos en DECIMAL: pesos con 2 decimales, BTC con 8 (1 satoshi).
-- Las tablas son pequeñas; MODIFY reescribe la tabla y la bloquea solo un momento.
-- Para revertir: restaurar el respaldo de mysqldump tomado antes de migrar.

ALTER TABLE trades
    MODIFY entry_price      DECIMAL(19, 2) NOT NULL,
    MODIFY exit_price       DECIMAL(19, 2) DEFAULT NULL,
    MODIFY invested_mxn     DECIMAL(19, 2) NOT NULL,
    MODIFY quantity         DECIMAL(19, 8) NOT NULL,
    MODIFY stop_loss        DECIMAL(19, 2) DEFAULT NULL,
    MODIFY take_profit      DECIMAL(19, 2) DEFAULT NULL,
    MODIFY profit_loss      DECIMAL(19, 2) DEFAULT NULL,
    MODIFY profit_loss_pct  DECIMAL(9, 4)  DEFAULT NULL;

ALTER TABLE signals_history
    MODIFY precio DECIMAL(19, 2) NOT NULL;

ALTER TABLE portfolio_snapshots
    MODIFY btc_balance      DECIMAL(19, 8) NOT NULL,
    MODIFY btc_price        DECIMAL(19, 2) NOT NULL,
    MODIFY mxn_balance      DECIMAL(19, 2) NOT NULL,
    MODIFY total_value_mxn  DECIMAL(19, 2) NOT NULL,
    MODIFY unrealized_pnl   DECIMAL(19, 2) DEFAULT NULL,
    MODIFY snapshot_at      DATETIME(6)    NOT NULL;

-- Consultas del bot: la operación abierta más reciente, el último snapshot y la última señal
CREATE INDEX idx_trades_status_opened_at ON trades (status, opened_at);
CREATE INDEX idx_trades_opened_at ON trades (opened_at);
CREATE INDEX idx_signals_history_timestamp ON signals_history (timestamp);
CREATE INDEX idx_portfolio_snapshots_snapshot_at ON portfolio_snapshots (snapshot_at);
