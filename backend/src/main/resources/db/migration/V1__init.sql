-- Esquema tal como lo dejó Hibernate (ddl-auto=update) antes de usar Flyway.
-- En una base que ya tiene estas tablas, Flyway la marca como aplicada (baseline-on-migrate).

CREATE TABLE trades (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    close_reason     VARCHAR(20)  DEFAULT NULL,
    closed_at        DATETIME(6)  DEFAULT NULL,
    entry_price      DOUBLE       NOT NULL,
    entry_signal_id  BIGINT       DEFAULT NULL,
    exit_price       DOUBLE       DEFAULT NULL,
    exit_signal_id   BIGINT       DEFAULT NULL,
    invested_mxn     DOUBLE       NOT NULL,
    opened_at        DATETIME(6)  NOT NULL,
    profit_loss      DOUBLE       DEFAULT NULL,
    profit_loss_pct  DOUBLE       DEFAULT NULL,
    quantity         DOUBLE       NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    stop_loss        DOUBLE       DEFAULT NULL,
    symbol           VARCHAR(20)  NOT NULL,
    take_profit      DOUBLE       DEFAULT NULL,
    trade_type       VARCHAR(10)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE signals_history (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    confianza           INT          NOT NULL,
    precio              DOUBLE       NOT NULL,
    score_buy           INT          DEFAULT NULL,
    score_sell          INT          DEFAULT NULL,
    senal               VARCHAR(50)  NOT NULL,
    strategy_breakdown  TEXT         DEFAULT NULL,
    timestamp           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE portfolio_snapshots (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    btc_balance      DOUBLE       NOT NULL,
    btc_price        DOUBLE       NOT NULL,
    mxn_balance      DOUBLE       NOT NULL,
    snapshot_at      DATETIME(6)  DEFAULT NULL,
    total_value_mxn  DOUBLE       NOT NULL,
    unrealized_pnl   DOUBLE       DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
