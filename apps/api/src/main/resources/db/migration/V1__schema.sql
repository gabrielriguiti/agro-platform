-- Schema completo da plataforma agro (ADR-0003: Flyway + ddl-auto=validate).
-- Convenção: tabelas e colunas em inglês; comentários (COMMENT ON) em português.
-- Inscrições fiscais (CPF/CNPJ) são armazenadas apenas com dígitos.

-- ---------------------------------------------------------------------------
-- Cadastro (contexto registry)
-- ---------------------------------------------------------------------------

CREATE TABLE laboratories (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    tax_id          VARCHAR(14)   NOT NULL,
    address         VARCHAR(500)  NOT NULL,
    external_source VARCHAR(100),
    external_id     VARCHAR(100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_laboratories_tax_id UNIQUE (tax_id),
    CONSTRAINT uq_laboratories_external UNIQUE (external_source, external_id),
    CONSTRAINT ck_laboratories_tax_id_digits CHECK (tax_id ~ '^[0-9]{14}$')
);

CREATE TABLE producers (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    tax_id          VARCHAR(14)   NOT NULL,
    address         VARCHAR(500)  NOT NULL,
    data_inicial    TIMESTAMPTZ   NOT NULL,
    data_final      TIMESTAMPTZ   NOT NULL,
    observacoes     TEXT,
    laboratory_id   BIGINT        NOT NULL REFERENCES laboratories (id),
    external_source VARCHAR(100),
    external_id     VARCHAR(100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_producers_tax_id UNIQUE (tax_id),
    CONSTRAINT uq_producers_external UNIQUE (external_source, external_id),
    CONSTRAINT ck_producers_tax_id_digits CHECK (tax_id ~ '^([0-9]{11}|[0-9]{14})$'),
    CONSTRAINT ck_producers_period CHECK (data_final >= data_inicial)
);

CREATE TABLE properties (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    tax_id          VARCHAR(14)   NOT NULL,
    address         VARCHAR(500)  NOT NULL,
    producer_id     BIGINT        REFERENCES producers (id) ON DELETE SET NULL,
    external_source VARCHAR(100),
    external_id     VARCHAR(100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_properties_tax_id UNIQUE (tax_id),
    CONSTRAINT uq_properties_external UNIQUE (external_source, external_id),
    CONSTRAINT ck_properties_tax_id_digits CHECK (tax_id ~ '^([0-9]{11}|[0-9]{14})$')
);

CREATE INDEX ix_producers_laboratory_id ON producers (laboratory_id);
CREATE INDEX ix_producers_data_inicial ON producers (data_inicial);
CREATE INDEX ix_producers_data_final ON producers (data_final);
CREATE INDEX ix_properties_producer_id ON properties (producer_id);

-- ---------------------------------------------------------------------------
-- Integração (contexto integration): lote e caixa de entrada durável
-- ---------------------------------------------------------------------------

CREATE TABLE integration_batch (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source         VARCHAR(100) NOT NULL,
    merge_strategy VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_integration_batch_strategy CHECK (merge_strategy IN ('MERGE', 'OVERWRITE', 'KEEP_EXISTING'))
);

CREATE TABLE outbox_message (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_id        BIGINT       NOT NULL REFERENCES integration_batch (id),
    idempotency_key VARCHAR(128) NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    external_id     VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    locked_until    TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_outbox_message_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_outbox_message_type CHECK (type IN ('PRODUCER', 'PROPERTY', 'LABORATORY')),
    CONSTRAINT ck_outbox_message_status CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'DEAD')),
    CONSTRAINT ck_outbox_message_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_outbox_message_status_next_attempt ON outbox_message (status, next_attempt_at);
CREATE INDEX ix_outbox_message_batch_id ON outbox_message (batch_id);

-- ---------------------------------------------------------------------------
-- Documentação do banco (COMMENT ON)
-- ---------------------------------------------------------------------------

COMMENT ON TABLE laboratories IS 'Laboratórios de análise; existem de forma autônoma e agrupam produtores.';
COMMENT ON COLUMN laboratories.id IS 'Identificador gerado pelo banco.';
COMMENT ON COLUMN laboratories.name IS 'Nome do laboratório.';
COMMENT ON COLUMN laboratories.tax_id IS 'CNPJ normalizado (14 dígitos, sem máscara); único.';
COMMENT ON COLUMN laboratories.address IS 'Endereço do laboratório.';
COMMENT ON COLUMN laboratories.external_source IS 'Sistema parceiro de origem do registro (integração).';
COMMENT ON COLUMN laboratories.external_id IS 'Identificador do registro no sistema parceiro; único junto com external_source.';
COMMENT ON COLUMN laboratories.created_at IS 'Instante de criação.';
COMMENT ON COLUMN laboratories.updated_at IS 'Instante da última alteração.';

COMMENT ON TABLE producers IS 'Produtores rurais; cada um pertence a exatamente um laboratório e pode ter várias propriedades.';
COMMENT ON COLUMN producers.id IS 'Identificador gerado pelo banco.';
COMMENT ON COLUMN producers.name IS 'Nome do produtor.';
COMMENT ON COLUMN producers.tax_id IS 'CPF ou CNPJ normalizado (11 ou 14 dígitos, sem máscara); único.';
COMMENT ON COLUMN producers.address IS 'Endereço do produtor.';
COMMENT ON COLUMN producers.data_inicial IS 'Início do período de relacionamento, em instante ISO-8601 (UTC).';
COMMENT ON COLUMN producers.data_final IS 'Fim do período de relacionamento; não pode ser anterior a data_inicial.';
COMMENT ON COLUMN producers.observacoes IS 'Observações livres (opcional).';
COMMENT ON COLUMN producers.laboratory_id IS 'Laboratório ao qual o produtor está vinculado.';
COMMENT ON COLUMN producers.external_source IS 'Sistema parceiro de origem do registro (integração).';
COMMENT ON COLUMN producers.external_id IS 'Identificador do registro no sistema parceiro; único junto com external_source.';
COMMENT ON COLUMN producers.created_at IS 'Instante de criação.';
COMMENT ON COLUMN producers.updated_at IS 'Instante da última alteração.';

COMMENT ON TABLE properties IS 'Propriedades rurais; existem sem produtor e pertencem a no máximo um.';
COMMENT ON COLUMN properties.id IS 'Identificador gerado pelo banco.';
COMMENT ON COLUMN properties.name IS 'Nome da propriedade.';
COMMENT ON COLUMN properties.tax_id IS 'CPF ou CNPJ normalizado (11 ou 14 dígitos, sem máscara); único.';
COMMENT ON COLUMN properties.address IS 'Endereço da propriedade.';
COMMENT ON COLUMN properties.producer_id IS 'Produtor dono da propriedade; nulo quando disponível. ON DELETE SET NULL libera a propriedade.';
COMMENT ON COLUMN properties.external_source IS 'Sistema parceiro de origem do registro (integração).';
COMMENT ON COLUMN properties.external_id IS 'Identificador do registro no sistema parceiro; único junto com external_source.';
COMMENT ON COLUMN properties.created_at IS 'Instante de criação.';
COMMENT ON COLUMN properties.updated_at IS 'Instante da última alteração.';

COMMENT ON TABLE integration_batch IS 'Lote de integração recebido de um sistema parceiro.';
COMMENT ON COLUMN integration_batch.id IS 'Identificador do lote (batchId devolvido na ingestão).';
COMMENT ON COLUMN integration_batch.source IS 'Sistema parceiro que enviou o lote.';
COMMENT ON COLUMN integration_batch.merge_strategy IS 'Estratégia de fusão do lote: MERGE, OVERWRITE ou KEEP_EXISTING.';
COMMENT ON COLUMN integration_batch.created_at IS 'Instante do recebimento do lote.';

COMMENT ON TABLE outbox_message IS 'Caixa de entrada durável (outbox): uma mensagem por item de lote, consumida pelo worker.';
COMMENT ON COLUMN outbox_message.id IS 'Identificador da mensagem.';
COMMENT ON COLUMN outbox_message.batch_id IS 'Lote ao qual a mensagem pertence.';
COMMENT ON COLUMN outbox_message.idempotency_key IS 'Hash de source, type, externalId e payload canônico; barra reenvios idênticos.';
COMMENT ON COLUMN outbox_message.type IS 'Tipo da entidade: PRODUCER, PROPERTY ou LABORATORY.';
COMMENT ON COLUMN outbox_message.external_id IS 'Identificador do item no sistema parceiro.';
COMMENT ON COLUMN outbox_message.payload IS 'Payload JSON original, preservado inclusive na DLQ.';
COMMENT ON COLUMN outbox_message.status IS 'Estado: PENDING, PROCESSING, DONE ou DEAD (dead letter).';
COMMENT ON COLUMN outbox_message.attempts IS 'Número de tentativas de processamento (incrementado na reserva).';
COMMENT ON COLUMN outbox_message.next_attempt_at IS 'Instante a partir do qual a mensagem é elegível (backoff).';
COMMENT ON COLUMN outbox_message.locked_until IS 'Fim da reserva (lease) do worker; reserva vencida volta a ser elegível.';
COMMENT ON COLUMN outbox_message.last_error IS 'Último erro registrado no processamento.';
COMMENT ON COLUMN outbox_message.created_at IS 'Instante de criação da mensagem.';
COMMENT ON COLUMN outbox_message.updated_at IS 'Instante da última alteração.';
