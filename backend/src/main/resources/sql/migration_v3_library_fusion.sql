-- ============================================================
-- 三库融合：模板库-标准库-知识库 链接表
-- 模板章节 ↔ 标准条款 ↔ 知识卡片 ↔ 主数据字段
-- ============================================================

-- ============================================================
-- V3-1: 章节-标准条款 多对多链接
-- 替代 doc_template_chapter.standard_clause_ref TEXT 字段的弱引用
-- ============================================================
CREATE TABLE IF NOT EXISTS template_chapter_clause_link (
    id                    BIGSERIAL PRIMARY KEY,
    template_chapter_id   BIGINT NOT NULL,
    standard_clause_id    BIGINT NOT NULL,
    link_type             VARCHAR(32) DEFAULT 'REFERENCES',
    relevance_note        TEXT,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tccl_chapter FOREIGN KEY (template_chapter_id)
        REFERENCES doc_template_chapter(id) ON DELETE CASCADE,
    CONSTRAINT fk_tccl_clause FOREIGN KEY (standard_clause_id)
        REFERENCES standard_clause(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tccl_pair ON template_chapter_clause_link(template_chapter_id, standard_clause_id);
CREATE INDEX IF NOT EXISTS idx_tccl_chapter ON template_chapter_clause_link(template_chapter_id);
CREATE INDEX IF NOT EXISTS idx_tccl_clause ON template_chapter_clause_link(standard_clause_id);
COMMENT ON TABLE template_chapter_clause_link IS '模板章节与标准条款的多对多关联';
COMMENT ON COLUMN template_chapter_clause_link.link_type IS 'REFERENCES-引用参考, REQUIRES-必须满足, INFORMS-提供信息';

-- ============================================================
-- V3-2: 章节-主数据字段映射
-- 定义每个模板章节需要哪些项目主数据字段
-- ============================================================
CREATE TABLE IF NOT EXISTS template_chapter_field_mapping (
    id                    BIGSERIAL PRIMARY KEY,
    template_chapter_id   BIGINT NOT NULL,
    master_data_path      VARCHAR(256) NOT NULL,
    field_label           VARCHAR(128),
    is_required           BOOLEAN DEFAULT FALSE,
    order_num             INT DEFAULT 0,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tcfm_chapter FOREIGN KEY (template_chapter_id)
        REFERENCES doc_template_chapter(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tcfm_pair ON template_chapter_field_mapping(template_chapter_id, master_data_path);
CREATE INDEX IF NOT EXISTS idx_tcfm_chapter ON template_chapter_field_mapping(template_chapter_id);
COMMENT ON TABLE template_chapter_field_mapping IS '模板章节所需主数据字段映射';
COMMENT ON COLUMN template_chapter_field_mapping.master_data_path IS '主数据JSON路径, 如 equipmentInfo.equipmentName, tacticalIndicators[]';

-- ============================================================
-- V3-3: 章节-知识卡片 多对多链接
-- 替代 knowledge_card 的多态 target_table/target_id 模式作为主查询路径
-- ============================================================
CREATE TABLE IF NOT EXISTS chapter_knowledge_card_link (
    id                    BIGSERIAL PRIMARY KEY,
    template_chapter_id   BIGINT NOT NULL,
    knowledge_card_id     BIGINT NOT NULL,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ckcl_chapter FOREIGN KEY (template_chapter_id)
        REFERENCES doc_template_chapter(id) ON DELETE CASCADE,
    CONSTRAINT fk_ckcl_card FOREIGN KEY (knowledge_card_id)
        REFERENCES knowledge_card(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ckcl_pair ON chapter_knowledge_card_link(template_chapter_id, knowledge_card_id);
CREATE INDEX IF NOT EXISTS idx_ckcl_chapter ON chapter_knowledge_card_link(template_chapter_id);
CREATE INDEX IF NOT EXISTS idx_ckcl_card ON chapter_knowledge_card_link(knowledge_card_id);
COMMENT ON TABLE chapter_knowledge_card_link IS '模板章节与知识卡片的多对多关联';

-- ============================================================
-- 迁移: 从现有 doc_template_chapter.standard_clause_ref TEXT字段
-- 模糊匹配 standard_clause 记录，填充 template_chapter_clause_link
-- ============================================================
DO $$
DECLARE
    tc RECORD;
    sc RECORD;
    clause_pattern TEXT;
BEGIN
    FOR tc IN
        SELECT id, template_id, standard_clause_ref
        FROM doc_template_chapter
        WHERE standard_clause_ref IS NOT NULL
          AND standard_clause_ref != ''
          AND deleted = 0
    LOOP
        -- 尝试从 "GJB/Z 170.4 5.1" 或 "GJB 6387 4.3.1" 格式中提取条款号
        -- 匹配模式: 数字.数字 或 数字.数字.数字
        clause_pattern := substring(tc.standard_clause_ref from '\d+\.\d+(\.\d+)?');

        IF clause_pattern IS NOT NULL AND clause_pattern != '' THEN
            FOR sc IN
                SELECT id FROM standard_clause
                WHERE clause_number LIKE '%' || clause_pattern || '%'
                  AND deleted = 0
                LIMIT 3
            LOOP
                INSERT INTO template_chapter_clause_link
                    (template_chapter_id, standard_clause_id, link_type)
                VALUES (tc.id, sc.id, 'REFERENCES')
                ON CONFLICT (template_chapter_id, standard_clause_id) DO NOTHING;
            END LOOP;
        END IF;
    END LOOP;
END $$;

-- ============================================================
-- 迁移: 从 knowledge_card 多态引用迁移到 chapter_knowledge_card_link
-- ============================================================
DO $$
DECLARE
    kc RECORD;
BEGIN
    FOR kc IN
        SELECT id, target_table, target_id
        FROM knowledge_card
        WHERE target_table = 'doc_template_chapter'
          AND target_id IS NOT NULL
          AND deleted = 0
    LOOP
        INSERT INTO chapter_knowledge_card_link
            (template_chapter_id, knowledge_card_id)
        VALUES (kc.target_id, kc.id)
        ON CONFLICT (template_chapter_id, knowledge_card_id) DO NOTHING;
    END LOOP;
END $$;
