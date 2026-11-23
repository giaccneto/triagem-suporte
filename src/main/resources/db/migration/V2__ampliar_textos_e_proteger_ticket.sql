ALTER TABLE chamado
    ALTER COLUMN message TYPE TEXT,
    ALTER COLUMN justificativa TYPE TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_chamado_ticket_id ON chamado (ticket_id);
