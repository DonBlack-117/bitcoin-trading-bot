-- Garantiza una sola operación OPEN a la vez y añade índices y checks de integridad.
-- Si alguna vez hubiera dos filas OPEN (p. ej. por una doble apertura previa), el índice único
-- fallaría al crearse: cerrar o corregir esas filas antes de aplicar esta migración.

-- Columna generada: 1 cuando la operación está abierta, NULL en otro caso.
-- El índice único sobre ella deja pasar una sola OPEN y permite varias CLOSED (NULL no colisiona).
ALTER TABLE trades
    ADD COLUMN open_flag TINYINT AS (IF(status = 'OPEN', 1, NULL)) PERSISTENT,
    ADD UNIQUE KEY uq_trades_one_open (open_flag);

-- Conteo de ganadoras en /portfolio: cubre (status, profit_loss).
CREATE INDEX idx_trades_status_pl ON trades (status, profit_loss);

-- El último snapshot se lee con ORDER BY snapshot_at DESC, id DESC: índice explícito para evitar filesort.
CREATE INDEX idx_portfolio_snapshots_snapshot_at_id ON portfolio_snapshots (snapshot_at, id);

-- Invariantes de dominio: estado válido y montos positivos.
ALTER TABLE trades
    ADD CONSTRAINT ck_trades_status CHECK (status IN ('OPEN', 'CLOSED')),
    ADD CONSTRAINT ck_trades_quantity CHECK (quantity > 0),
    ADD CONSTRAINT ck_trades_invested CHECK (invested_mxn > 0);
