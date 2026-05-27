-- ============================================================
-- 数据完整性 + 性能优化: 补充外键约束和缺失索引
-- ============================================================

-- ============================================================
-- Part 1: 外键约束 (先清理可能的孤儿数据，再添加约束)
-- ============================================================

-- 系统模块
ALTER TABLE sys_user_role
    DROP CONSTRAINT IF EXISTS fk_sur_user,
    ADD CONSTRAINT fk_sur_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

ALTER TABLE sys_user_role
    DROP CONSTRAINT IF EXISTS fk_sur_role,
    ADD CONSTRAINT fk_sur_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE;

ALTER TABLE sys_role_permission
    DROP CONSTRAINT IF EXISTS fk_srp_role,
    ADD CONSTRAINT fk_srp_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE;

ALTER TABLE sys_role_permission
    DROP CONSTRAINT IF EXISTS fk_srp_permission,
    ADD CONSTRAINT fk_srp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE;

-- 项目模块
ALTER TABLE project_member
    DROP CONSTRAINT IF EXISTS fk_pm_project,
    ADD CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE project_member
    DROP CONSTRAINT IF EXISTS fk_pm_user,
    ADD CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

ALTER TABLE project_stage
    DROP CONSTRAINT IF EXISTS fk_ps_project,
    ADD CONSTRAINT fk_ps_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE stage_transition_check
    DROP CONSTRAINT IF EXISTS fk_stc_project,
    ADD CONSTRAINT fk_stc_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE stage_transition_check
    DROP CONSTRAINT IF EXISTS fk_stc_from_stage,
    ADD CONSTRAINT fk_stc_from_stage FOREIGN KEY (from_stage_id) REFERENCES project_stage(id) ON DELETE SET NULL;

ALTER TABLE stage_transition_check
    DROP CONSTRAINT IF EXISTS fk_stc_to_stage,
    ADD CONSTRAINT fk_stc_to_stage FOREIGN KEY (to_stage_id) REFERENCES project_stage(id) ON DELETE SET NULL;

-- 文档模块
ALTER TABLE doc_catalog
    DROP CONSTRAINT IF EXISTS fk_dc_project,
    ADD CONSTRAINT fk_dc_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE doc_catalog
    DROP CONSTRAINT IF EXISTS fk_dc_stage,
    ADD CONSTRAINT fk_dc_stage FOREIGN KEY (stage_id) REFERENCES project_stage(id) ON DELETE SET NULL;

ALTER TABLE doc_file
    DROP CONSTRAINT IF EXISTS fk_df_catalog,
    ADD CONSTRAINT fk_df_catalog FOREIGN KEY (catalog_id) REFERENCES doc_catalog(id) ON DELETE SET NULL;

ALTER TABLE doc_file
    DROP CONSTRAINT IF EXISTS fk_df_project,
    ADD CONSTRAINT fk_df_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE doc_version
    DROP CONSTRAINT IF EXISTS fk_dv_docfile,
    ADD CONSTRAINT fk_dv_docfile FOREIGN KEY (doc_file_id) REFERENCES doc_file(id) ON DELETE CASCADE;

ALTER TABLE doc_edit_session
    DROP CONSTRAINT IF EXISTS fk_des_docfile,
    ADD CONSTRAINT fk_des_docfile FOREIGN KEY (doc_file_id) REFERENCES doc_file(id) ON DELETE CASCADE;

ALTER TABLE doc_edit_lock
    DROP CONSTRAINT IF EXISTS fk_del_docfile,
    ADD CONSTRAINT fk_del_docfile FOREIGN KEY (doc_file_id) REFERENCES doc_file(id) ON DELETE CASCADE;

ALTER TABLE doc_ledger
    DROP CONSTRAINT IF EXISTS fk_dl_project,
    ADD CONSTRAINT fk_dl_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE doc_ledger_log
    DROP CONSTRAINT IF EXISTS fk_dll_ledger,
    ADD CONSTRAINT fk_dll_ledger FOREIGN KEY (doc_ledger_id) REFERENCES doc_ledger(id) ON DELETE CASCADE;

ALTER TABLE doc_chapter
    DROP CONSTRAINT IF EXISTS fk_dchap_ledger,
    ADD CONSTRAINT fk_dchap_ledger FOREIGN KEY (doc_ledger_id) REFERENCES doc_ledger(id) ON DELETE CASCADE;

ALTER TABLE completeness_check_result
    DROP CONSTRAINT IF EXISTS fk_ccr_project,
    ADD CONSTRAINT fk_ccr_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE completeness_check_result
    DROP CONSTRAINT IF EXISTS fk_ccr_ledger,
    ADD CONSTRAINT fk_ccr_ledger FOREIGN KEY (doc_ledger_id) REFERENCES doc_ledger(id) ON DELETE SET NULL;

-- 评审模块
ALTER TABLE review_meeting_document
    DROP CONSTRAINT IF EXISTS fk_rmd_meeting,
    ADD CONSTRAINT fk_rmd_meeting FOREIGN KEY (meeting_id) REFERENCES review_meeting(id) ON DELETE CASCADE;

ALTER TABLE review_meeting_document
    DROP CONSTRAINT IF EXISTS fk_rmd_docfile,
    ADD CONSTRAINT fk_rmd_docfile FOREIGN KEY (doc_file_id) REFERENCES doc_file(id) ON DELETE CASCADE;

ALTER TABLE review_expert_opinion_file
    DROP CONSTRAINT IF EXISTS fk_reof_meeting,
    ADD CONSTRAINT fk_reof_meeting FOREIGN KEY (meeting_id) REFERENCES review_meeting(id) ON DELETE CASCADE;

-- 模板模块
ALTER TABLE doc_template_v2
    DROP CONSTRAINT IF EXISTS fk_dtv2_category,
    ADD CONSTRAINT fk_dtv2_category FOREIGN KEY (category_id) REFERENCES doc_template_category(id) ON DELETE RESTRICT;

ALTER TABLE doc_template_chapter
    DROP CONSTRAINT IF EXISTS fk_dtch_template,
    ADD CONSTRAINT fk_dtch_template FOREIGN KEY (template_id) REFERENCES doc_template_v2(id) ON DELETE CASCADE;

ALTER TABLE doc_template_chapter_element
    DROP CONSTRAINT IF EXISTS fk_dtce_chapter,
    ADD CONSTRAINT fk_dtce_chapter FOREIGN KEY (chapter_id) REFERENCES doc_template_chapter(id) ON DELETE CASCADE;

ALTER TABLE doc_template_chapter_element
    DROP CONSTRAINT IF EXISTS fk_dtce_element,
    ADD CONSTRAINT fk_dtce_element FOREIGN KEY (element_id) REFERENCES doc_template_element(id) ON DELETE CASCADE;

-- 配置管理
ALTER TABLE configuration_baseline
    DROP CONSTRAINT IF EXISTS fk_cb_project,
    ADD CONSTRAINT fk_cb_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE configuration_baseline_item
    DROP CONSTRAINT IF EXISTS fk_cbi_baseline,
    ADD CONSTRAINT fk_cbi_baseline FOREIGN KEY (baseline_id) REFERENCES configuration_baseline(id) ON DELETE CASCADE;

ALTER TABLE configuration_change_request
    DROP CONSTRAINT IF EXISTS fk_ccr_project,
    ADD CONSTRAINT fk_ccr_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE configuration_status_accounting
    DROP CONSTRAINT IF EXISTS fk_csa_project,
    ADD CONSTRAINT fk_csa_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE configuration_audit
    DROP CONSTRAINT IF EXISTS fk_ca_project,
    ADD CONSTRAINT fk_ca_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

ALTER TABLE configuration_item
    DROP CONSTRAINT IF EXISTS fk_ci_project,
    ADD CONSTRAINT fk_ci_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;

-- AI训练
ALTER TABLE ai_training_example
    DROP CONSTRAINT IF EXISTS fk_ate_project,
    ADD CONSTRAINT fk_ate_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL;

ALTER TABLE ai_training_example
    DROP CONSTRAINT IF EXISTS fk_ate_docfile,
    ADD CONSTRAINT fk_ate_docfile FOREIGN KEY (doc_file_id) REFERENCES doc_file(id) ON DELETE SET NULL;

-- ============================================================
-- Part 2: 缺失索引 (常用查询列)
-- ============================================================

-- 用户/角色状态过滤
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON sys_user(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_role_status ON sys_role(status) WHERE deleted = 0;

-- 项目负责人
CREATE INDEX IF NOT EXISTS idx_project_owner ON project(owner_user_id) WHERE deleted = 0;

-- 文档版本复合索引 (最常用的查询: 找某文件的最新版本)
CREATE INDEX IF NOT EXISTS idx_dv_file_status ON doc_version(doc_file_id, version_status) WHERE deleted = 0;

-- 文档台账按编号排序
CREATE INDEX IF NOT EXISTS idx_dl_code ON doc_ledger(doc_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dl_responsible ON doc_ledger(responsible_user_id) WHERE deleted = 0;

-- 评审会议文件反向查询
CREATE INDEX IF NOT EXISTS idx_rmd_docfile ON review_meeting_document(doc_file_id) WHERE deleted = 0;

-- 配置管理状态过滤
CREATE INDEX IF NOT EXISTS idx_ccr_status ON configuration_change_request(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_cb_status ON configuration_baseline(baseline_status) WHERE deleted = 0;

-- 文档目录状态过滤
CREATE INDEX IF NOT EXISTS idx_dc_status ON doc_catalog(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dc_project_stage ON doc_catalog(project_id, stage_id) WHERE deleted = 0;

-- 嵌入索引任务
CREATE INDEX IF NOT EXISTS idx_eit_status ON embedding_index_task(task_status) WHERE deleted = 0;
