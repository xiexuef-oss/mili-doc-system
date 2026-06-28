-- Add doc_content field to doc_ledger for storing AI-generated document content
ALTER TABLE doc_ledger ADD COLUMN IF NOT EXISTS doc_content TEXT;

COMMENT ON COLUMN doc_ledger.doc_content IS 'AI生成的文档内容(Markdown格式)';