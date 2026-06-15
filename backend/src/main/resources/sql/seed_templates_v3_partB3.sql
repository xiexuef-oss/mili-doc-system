-- ============================================================
-- Part B3: 系统规范A类 + 研制规范B类 + 产品规范C类 (GJB 6387 六章固定格式)
-- ============================================================

-- ========================================
-- 系统规范 A类 (TPL-A-SPEC) — GJB 6387-2008 Ch6
-- ========================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-A-SPEC';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387 4.2.3.2', '系统规范的适用范围，适用的系统和阶段',
     '简要说明本规范的主题内容、适用范围和不适用范围，不超过100字') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 6387 4.2.3.2', '本规范引用的所有标准、规范和其他文件',
     '列出文件代号、名称和版本。注明"下列文件中的条款通过本规范的引用而成为本规范的条款"') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387 4.2.3.2', '系统必须满足的所有功能特性、性能特性和接口要求',
     '这是A类规范的核心章。要求必须是可度量和可验证的。使用"应"来表达要求型条款') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '系统定义', 2, 31, TRUE, 'GJB 6387', '系统的总体描述、组成和作战使命');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', '系统功能', 2, 32, TRUE, 'GJB 6387', '系统应具备的主要功能和辅助功能');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.3', '性能特性', 2, 33, TRUE, 'GJB 6387', '系统的主要战术技术性能指标');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.4', '物理特性', 2, 34, TRUE, 'GJB 6387', '尺寸、重量、防护等级等物理约束');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.5', '系统接口', 2, 35, TRUE, 'GJB 6387', '系统与其他系统、平台之间的接口要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.6', '环境适应性', 2, 36, TRUE, 'GJB 6387', '系统应满足的自然环境和诱生环境条件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.7', '电磁兼容性', 2, 37, TRUE, 'GJB 6387', '系统电磁发射和敏感度要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.8', '可靠性', 2, 38, TRUE, 'GJB 6387', '系统可靠性定量定性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.9', '维修性', 2, 39, TRUE, 'GJB 6387', '系统维修性定量定性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.10', '安全性', 2, 310, TRUE, 'GJB 6387', '系统安全性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.11', '保障性', 2, 311, FALSE, 'GJB 6387', '系统综合保障要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.12', '标准化要求', 2, 312, FALSE, 'GJB 6387', '三化(通用化/系列化/组合化)要求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387 4.2.3.2', '验证第3章各项要求的方法和合格判据',
     '每一个要求条款都应有对应的验证方法: 分析/演示/试验/检查') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '验证矩阵', 2, 41, TRUE, 'GJB 6387', '要求条款与验证方法的对应关系表');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '验证方法', 2, 42, TRUE, 'GJB 6387', '分析、演示、试验、检查四类验证方法的说明');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '合格判据', 2, 43, TRUE, 'GJB 6387', '各项验证的通过/不通过标准和数据要求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '包装、运输与贮存', 1, 50, TRUE, 'GJB 6387 4.2.3.2',
     '产品的防护包装、装箱、运输和贮存要求') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '防护包装', 2, 51, TRUE, 'GJB 6387', '包装等级、包装材料和包装方法');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', '运输要求', 2, 52, TRUE, 'GJB 6387', '运输方式、运输环境条件和运输过程中的防护');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.3', '贮存要求', 2, 53, TRUE, 'GJB 6387', '贮存环境条件、贮存期限和贮存期间的维护');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '说明事项', 1, 60, TRUE, 'GJB 6387 4.2.3.2',
     '预定用途、分类、订购文件中应明确的内容、术语和定义') RETURNING id INTO ch6;
END $$;
