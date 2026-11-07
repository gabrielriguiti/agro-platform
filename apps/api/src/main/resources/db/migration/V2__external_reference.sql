-- Vínculos externos (change integration-pipeline, ADR-0013): um registro pode ser conhecido por vários sistemas
-- parceiros, então o par único external_source/external_id do V1 (um parceiro por registro) é substituído por
-- uma tabela de vínculos. As colunas antigas nunca foram mapeadas nem preenchidas.

ALTER TABLE laboratories DROP COLUMN external_source, DROP COLUMN external_id;
ALTER TABLE producers    DROP COLUMN external_source, DROP COLUMN external_id;
ALTER TABLE properties   DROP COLUMN external_source, DROP COLUMN external_id;

CREATE TABLE external_reference (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type VARCHAR(20)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    source      VARCHAR(100) NOT NULL,
    external_id VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_external_reference UNIQUE (entity_type, source, external_id),
    CONSTRAINT ck_external_reference_type CHECK (entity_type IN ('PRODUCER', 'PROPERTY', 'LABORATORY'))
);

CREATE INDEX ix_external_reference_entity ON external_reference (entity_type, entity_id);

COMMENT ON TABLE external_reference IS 'Vínculo entre um registro do cadastro e seu identificador em um sistema parceiro; um registro pode ter vários.';
COMMENT ON COLUMN external_reference.id IS 'Identificador gerado pelo banco.';
COMMENT ON COLUMN external_reference.entity_type IS 'Tipo do registro: PRODUCER, PROPERTY ou LABORATORY.';
COMMENT ON COLUMN external_reference.entity_id IS 'Id do registro na tabela do tipo (sem FK, pois a tabela depende do tipo).';
COMMENT ON COLUMN external_reference.source IS 'Sistema parceiro dono do identificador.';
COMMENT ON COLUMN external_reference.external_id IS 'Identificador do registro no sistema parceiro; único por tipo e parceiro (chave do upsert).';
COMMENT ON COLUMN external_reference.created_at IS 'Instante em que o vínculo foi criado.';
