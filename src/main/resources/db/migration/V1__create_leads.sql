CREATE TABLE leads
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(160) NOT NULL,
    email      VARCHAR(320) NOT NULL,
    phone      VARCHAR(32),
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_leads_email UNIQUE (email),
    CONSTRAINT ck_leads_status CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED', 'REJECTED'))
);

CREATE INDEX ix_leads_created_at_id ON leads (created_at, id);
