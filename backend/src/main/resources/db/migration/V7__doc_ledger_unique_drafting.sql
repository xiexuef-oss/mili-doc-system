-- Prevent concurrent findOrCreateDraftLedger from creating duplicate DRAFTING ledgers per (project_id, catalog_id)
CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_ledger_drafting ON doc_ledger(project_id, catalog_id) WHERE lifecycle_status = 'DRAFTING' AND deleted = 0;
