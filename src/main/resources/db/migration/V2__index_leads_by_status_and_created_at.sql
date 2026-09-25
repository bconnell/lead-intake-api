CREATE INDEX ix_leads_status_created_at_id ON leads (status, created_at DESC, id DESC);
