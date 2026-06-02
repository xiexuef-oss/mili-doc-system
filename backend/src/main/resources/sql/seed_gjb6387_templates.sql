-- ============================================================
-- GJB 6387-2008 六类规范完整写作模板
-- 基于 GJB 6387-2008 第6-9章要素定义
-- 为系统/研制/产品规范创建34要素章节结构
-- ============================================================

-- ============================================================
-- 1. 确保六类模板记录存在 (A_SPEC/B_SPEC/C_SPEC已存在类别)
-- ============================================================

-- B类规范(研制规范) 模板
INSERT INTO doc_template_v2(category_id, template_code, template_name, template_type, applicable_stage_codes, gjb_standard_ref, document_class, variables_schema, status)
SELECT id, 'TPL-B-SPEC', '研制规范(B类)', 'B_SPEC', 'F,C', 'GJB 6387-2008', 'B_SPEC',
       '{"systemName":{"type":"string","label":"系统名称","required":true}}',
       'ACTIVE'
FROM doc_template_category WHERE category_code = 'B_SPEC'
AND NOT EXISTS (SELECT 1 FROM doc_template_v2 WHERE template_code = 'TPL-B-SPEC');

-- D类规范(工艺规范) 模板
INSERT INTO doc_template_v2(category_id, template_code, template_name, template_type, applicable_stage_codes, gjb_standard_ref, document_class, variables_schema, status)
SELECT id, 'TPL-D-SPEC', '工艺规范(D类)', 'D_SPEC', 'F,C', 'GJB 6387-2008', 'D_SPEC',
       '{"processName":{"type":"string","label":"工艺名称","required":true}}',
       'ACTIVE'
FROM doc_template_category WHERE category_code = 'D_SPEC'
AND NOT EXISTS (SELECT 1 FROM doc_template_v2 WHERE template_code = 'TPL-D-SPEC');

-- E类规范(材料规范) 模板
INSERT INTO doc_template_v2(category_id, template_code, template_name, template_type, applicable_stage_codes, gjb_standard_ref, document_class, variables_schema, status)
SELECT id, 'TPL-E-SPEC', '材料规范(E类)', 'E_SPEC', 'F,C', 'GJB 6387-2008', 'E_SPEC',
       '{"materialName":{"type":"string","label":"材料名称","required":true}}',
       'ACTIVE'
FROM doc_template_category WHERE category_code = 'E_SPEC'
AND NOT EXISTS (SELECT 1 FROM doc_template_v2 WHERE template_code = 'TPL-E-SPEC');

-- 软件系统规格说明 模板
INSERT INTO doc_template_v2(category_id, template_code, template_name, template_type, applicable_stage_codes, gjb_standard_ref, document_class, variables_schema, status)
SELECT id, 'TPL-SW-SSS', '系统规格说明(软件)', 'SOFTWARE', 'F', 'GJB 438B-2009', 'SOFTWARE',
       '{"softwareSystemName":{"type":"string","label":"软件系统名称","required":true}}',
       'ACTIVE'
FROM doc_template_category WHERE category_code = 'SOFTWARE'
AND NOT EXISTS (SELECT 1 FROM doc_template_v2 WHERE template_code = 'TPL-SW-SSS');

-- ============================================================
-- 2. 系统规范(A类) — 完整34要素章节结构
-- ============================================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-A-SPEC';

    -- 删除旧的第3章简单结构，重新创建完整要素
    DELETE FROM doc_template_chapter WHERE template_id = tpl_id;

    -- 第1章 范围
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387 6.1', '规定本规范的主题内容和适用范围', '按5.9 a)格式："本规范规定了×××[系统代号和/或名称]的要求。"')
    RETURNING id INTO ch1;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.1', '主题内容', 2, 11, TRUE, 'GJB 6387 6.1.1', '明确规范的主题内容');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.2', '实体说明', 2, 12, FALSE, 'GJB 6387 6.1.2', '简要描述系统在工作分解结构中的层次，列出主要组成');

    -- 第2章 引用文件
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, FALSE, 'GJB 6387 5.10', '列出规范中引用的所有文件', '引用的文件排列顺序：国标→国军标→行业标准→部门军标→企业标准')
    RETURNING id INTO ch2;

    -- 第3章 要求 (34个要素，按GJB 6387 6.3定义)
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387 6.3', '规定系统必须满足的各项要求，源于使用需求或系统工程过程输出')
    RETURNING id INTO ch3;

    -- 以下创建34个要素章节，编号3.1-3.34

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.1', '作战能力/功能', 2, 31, TRUE, 'GJB 6387 6.3.1',
     '作战能力是系统在一定条件下完成作战使命任务能力的综合反映。通用要素包括：打击和拦截能力、作战保障能力、特种保障能力、一体化联合作战能力',
     '规定作战使命任务、作战使用方式。需明确指挥关系、协同方式、人员编成及各种状态与方式(空载/准备/战斗/训练/紧急备用等)');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.2', '性能', 2, 32, TRUE, 'GJB 6387 6.3.2',
     '规定表征实体能力的指标要求，包括参数值及其使用条件下的允许偏差。例如飞机的作战半径、雷达的射频工作频率、导弹的射程和命中精度',
     '每个性能指标应提出标称值或额定值及其允许偏差。适用时规定意外条件下的运行特征和防误操作措施');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.3', '作战适用性', 2, 33, TRUE, 'GJB 6387 6.3.3',
     '规定实体投入战场使用的满意程度，与可靠性、维修性、保障性、测试性、耐久性、安全性、兼容性、环境适应性等因素有关',
     '可从战备完好性、任务成功性、服役期限三类参数中选择适用参数。例如可用度、装备完好率、任务可靠度、服役期限等');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.4', '环境适应性', 2, 34, TRUE, 'GJB 6387 6.3.4',
     '规定实体在其寿命周期预计可能遇到的各种环境作用下能实现所有预定功能和不被破坏的能力',
     '环境条件包括：自然环境(气象/水文/地理)、特殊环境(核/化学/生物/电磁/光波)、诱发环境(冲击/振动/倾斜/摇摆/噪声/高温)');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.5', '可靠性', 2, 35, TRUE, 'GJB 6387 6.3.5',
     '规定实体在无故障、无退化或不要求保障系统保障的情况下执行其功能的能力。可靠性参数宜按GJB 1909的规定选取',
     '确定可靠性指标时需明确：寿命剖面、任务剖面、故障判别准则、维修方案、验证方法(含置信水平和接收/拒收判据)、达标时间。可用目标值和门限值表示');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.6', '维修性', 2, 36, TRUE, 'GJB 6387 6.3.6',
     '规定实体在规定的维修条件和时间内，按规定的程序和方法进行维修时，保持和恢复到规定状态的能力。参数宜按GJB 1909选取',
     '同样需明确寿命剖面、任务剖面、故障判别准则、维修方案、验证方法等。可用目标值和门限值表示');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.7', '保障性', 2, 37, TRUE, 'GJB 6387 6.3.7',
     '规定实体的设计特性和计划的保障资源满足平时战备完好性和战时利用率要求的能力',
     '保障性设计参数及保障资源参数宜按GJB 3872选取。可用目标值和门限值表示');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.8', '测试性', 2, 38, TRUE, 'GJB 6387 6.3.8',
     '规定实体及时准确地确定其状态(可工作/不可工作/性能下降)并隔离内部故障的能力。参数宜按GJB 1909选取',
     '明确与检测/隔离/报告故障有关的诊断能力：机内测试(BIT)、自动测试、手工测试、维修辅助手段、技术资料、人员和培训等');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.9', '耐久性', 2, 39, TRUE, 'GJB 6387 6.3.9',
     '规定实体在规定的使用、贮存与维修条件下达到极限状态之前完成规定功能的能力',
     '耐久性参数可包括：有用寿命、经济寿命、贮存寿命、总寿命、首翻期与翻修间隔期限。需明确实体类别和维修/贮存方案');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.10', '安全性', 2, 310, TRUE, 'GJB 6387 6.3.10',
     '规定实体以可接受的风险执行规定功能的能力，以及防止危害性事故发生的设计约束条件',
     '包括：安全性特征、"失效保险"和紧急操作约束、健康与安全准则(含有害物质)、软件防无意识动作措施、报警和事故预防措施、核安全等');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.11', '信息安全', 2, 311, TRUE, 'GJB 6387 6.3.11',
     '规定实体在警戒/情报/指挥/控制/通信/对抗等重要系统中以可接受的风险执行规定功能的能力',
     '包括：密码保护、安全防护(加扰/屏蔽)、计算机安全(隔离/容错/防病毒/防入侵/防复制)、访问控制、信息交换控制、人员控制');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.12', '隐蔽性', 2, 312, FALSE, 'GJB 6387 6.3.12',
     '规定实体的物理场不易被敌方发现、跟踪、识别的能力',
     '可用以下参数表示：雷达波反射、电磁辐射、声辐射、光辐射、红外辐射、放射性辐射、磁特性、声目标强度等。需明确实体与相关物理场的技术状态和隐蔽措施');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.13', '兼容性', 2, 313, TRUE, 'GJB 6387 6.3.13',
     '规定实体与其处于同一系统或同一环境中的其他实体互不干扰的能力',
     '包括：电磁兼容性(GJB 151/GJB 1389)、声兼容性、火力兼容性。需明确电磁发射和敏感度限值、噪声限值、时间安全域和空间安全域');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.14', '运输性', 2, 314, FALSE, 'GJB 6387 6.3.14',
     '规定实体自行或借助牵引/运载工具，利用铁路/公路/水路/海上/空中/空间等任何方式有效转移的能力',
     '明确运输方式、运输工具、流动路线、部署地点和装卸能力。需考虑实体要素和保障项目的限定条件');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.15', '人机工程', 2, 315, FALSE, 'GJB 6387 6.3.15',
     '规定实体和与之相关的人与环境的要求，以及三者之间的相互关系和作用方式，以最优组合获取最佳综合效能',
     '参照GJB 2873。通用要求：人机接口、工作环境(照明/颜色/温度/湿度/噪声/振动)、工作强度。专用要求：关键操作约束、错误预防纠正、特定环境特殊要求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.16', '互换性', 2, 316, FALSE, 'GJB 6387 6.3.16',
     '规定实体在尺寸和功能上与其他一个或多个产品(包括零部件)能够彼此互相替换的能力',
     '明确实体的设计条件和完成规定层次替换所需的时间');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.17', '稳定性', 2, 317, FALSE, 'GJB 6387 6.3.17',
     '规定实体控制理化性能变化以满足其预定用途及预定寿命所必需的能力',
     '稳定性参数可包括抗老化、抗腐蚀、抗倾覆等。需明确对应的理化性能、环境适应性和贮存/使用寿命');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.18', '综合保障', 2, 318, TRUE, 'GJB 6387 6.3.18',
     '规定在实体寿命周期内综合考虑保障问题，确定保障性要求，规划保障并研制保障资源，建立保障系统',
     '包括：规划使用保障(动用准备/运输/贮存/诊断方案)、规划维修(维修级别/原则/范围)、设计接口、保障资源(设备/供应/包装/计算机/技术资料/设施/人力/训练)');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.19', '接口', 2, 319, TRUE, 'GJB 6387 6.3.19',
     '规定实体的外部接口和内部接口，即实体与其他实体之间以及实体内部各组成部分之间共同边界上的诸多特性',
     '接口特性包括功能/电气电子/机械/介质/光学/信息/软件等。尽量采用标准接口或通用接口。宜引用接口控制文件(ICD)');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.20', '经济可承受性', 2, 320, FALSE, 'GJB 6387 6.3.20',
     '规定实体的寿命周期费用应在用户的经济承受能力之内，以影响设计权衡',
     '寿命周期费用包括论证费、研制费、采购费、使用与保障费、退役与处置费');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.21', '计算机硬件与软件', 2, 321, FALSE, 'GJB 6387 6.3.21',
     '规定实体对计算机硬件和软件的要求',
     '硬件要求：处理器能力、主存储器、输入/输出设备、辅助存储器、通信/网络、故障检测隔离冗余。软件要求：运行能力、综合显示、运行周期、灵活性、实时性、可重用性、可移植性、可测试性、人机界面');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.22', '尺寸和体积', 2, 322, FALSE, 'GJB 6387 6.3.22',
     '规定实体在外形尺寸和体积上的限制性定量要求、允许偏差与配合要求',
     '必要时规定实体的体积中心位置要求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.23', '重量', 2, 323, FALSE, 'GJB 6387 6.3.23',
     '规定实体在重量上的限制性定量要求与允许偏差要求',
     '必要时规定实体的重心位置要求以及实体各组成部分的重量要求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.24', '颜色', 2, 324, FALSE, 'GJB 6387 6.3.24',
     '从安全性、警示性、隐蔽性、耐脏性、协调性、舒适性和美观性等方面规定实体颜色的限制性要求',
     '可能时规定对应的定量要求，例如孟塞尔明度');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.25', '抗核加固', 2, 325, FALSE, 'GJB 6387 6.3.25',
     '规定可能在受核攻击的情况下执行关键任务的实体的抗核加固要求',
     '仅适用于需要在核环境下执行任务的实体');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.26', '理化性能', 2, 326, FALSE, 'GJB 6387 6.3.26',
     '规定实体的理化性能要求',
     '包括成分、浓度、硬度、强度、延伸率、热膨胀系数、电阻率及其他类似性能');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.27', '能耗', 2, 327, FALSE, 'GJB 6387 6.3.27',
     '规定实体直接消耗能源的品种、参数及能耗指标',
     '必要时规定实体重要组成部分的能耗指标');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.28', '材料', 2, 328, FALSE, 'GJB 6387 6.3.28',
     '依据实体的预定用途与性能以及人体健康与环境保护的要求，规定实体所用材料的限制性要求',
     '包括：性能要求(抗拉强度/硬度/冲击值/疲劳强度/工艺性)、防腐性、阻燃性、防电化学腐蚀、无毒或低毒、时效性');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.29', '非研制项目', 2, 329, FALSE, 'GJB 6387 6.3.29',
     '规定实体采用非研制项目(含标准零部件、组件)的要求',
     '非研制项目指已定型或可直接采购的货架产品');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.30', '外观质量', 2, 330, FALSE, 'GJB 6387 6.3.30',
     '规定实体的表面粗糙度、波纹度、防护涂镀层、缺陷、锈蚀、毛刺、机械伤痕、裂纹、表面加工均匀性等外观质量要求',
     '提出的要求应能作为判断实体外观质量是否合格的依据');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.31', '标志和代号', 2, 331, FALSE, 'GJB 6387 6.3.31',
     '规定实体标志和代号的要求',
     '标志包括：位置、内容(型号或标记/制造日期或生产批号)及顺序和制作要求。代号应简短，一般不超过15个字符');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.32', '主要组成部分特性', 2, 332, FALSE, 'GJB 6387 6.3.32',
     '规定实体各主要组成部分的性能特性要求和物理特性要求',
     '必要时设下级子条分别描述各组成部分，并说明交付安装后可能需要的检验');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.33', '图样和技术文件', 2, 333, FALSE, 'GJB 6387 6.3.33',
     '列出生产(含加工和装配)用的生产图样和技术文件',
     '典型表述："应对XXX(实体名称)提供下列生产图样和技术文件(含编号及名称):"');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch3, '3.34', '标准样件', 2, 334, FALSE, 'GJB 6387 6.3.34',
     '适用时规定标准样件，说明标准样件所应展示的具体特性以及可观察到的程度',
     '标准样件应尽量少用，只用来描述难以用试验程序或设计数据描述的品质特性(如皮毛纹理、织物颜色、木材细度等)');

    -- 第4章 验证
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387 6.4',
     '规定验证实体是否符合第3章和第5章所有要求的检验要求',
     '包括：检验分类、检验条件、设计验证、定型(鉴定)试验、首件检验、质量一致性检验、其他检验、包装检验、抽样、缺陷分类、检验方法')
    RETURNING id INTO ch4;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '检验分类', 2, 41, TRUE, 'GJB 6387 6.4.1', '确定检验类别及其组合，遵循代表性/经济性/快速性/再现性原则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '检验条件', 2, 42, TRUE, 'GJB 6387 6.4.2', '规定进行各种检验的环境条件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '设计验证', 2, 43, FALSE, 'GJB 6387 6.4.3', '通过模型和仿真验证、演示验证和系统联调试验验证设计方案');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '定型(鉴定)试验', 2, 44, TRUE, 'GJB 6387 6.4.4', '规定定型试验的检验项目、顺序、受检样品数及合格判据');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.5', '首件检验', 2, 45, FALSE, 'GJB 6387 6.4.5', '规定首件检验的项目、顺序、受检样品数及合格判据');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.6', '质量一致性检验', 2, 46, FALSE, 'GJB 6387 6.4.6', '规定质量一致性检验项目、分组、受检样品数及合格判据');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.7', '抽样', 2, 47, FALSE, 'GJB 6387 6.4.9', '确定组批规则、抽样方案(AQL等)、抽样条件和抽样方法');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.8', '缺陷分类', 2, 48, FALSE, 'GJB 6387 6.4.10', '缺陷分类及编码：1-99致命/101-199严重/201-299轻缺陷');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.9', '检验方法', 2, 49, TRUE, 'GJB 6387 6.4.11', '规定检验方法(分析法/演示法/检查法/模拟法/试验法)，含原理、设备、程序、故障处理、结果说明');

    -- 第5章 包装、运输与贮存
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '5', '包装、运输与贮存', 1, 50, FALSE, 'GJB 6387 6.5',
     '规定防护包装、装箱、运输、贮存和标志要求',
     '若有适用的现行标准(GJB 1181等)，直接引用或剪裁使用')
    RETURNING id INTO ch5;

    -- 第6章 说明事项
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '说明事项', 1, 60, FALSE, 'GJB 6387 6.6',
     '提供说明性信息，不应规定要求。包含：预定用途、分类、订购文件中应明确的内容、术语和定义、符号代号和缩略语、其他')
    RETURNING id INTO ch6;

    RAISE NOTICE 'A_SPEC template chapters created for template_id=%', tpl_id;
END $$;

-- ============================================================
-- 3. 研制规范(B类) — 与系统规范相同的34要素章节结构
-- ============================================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-B-SPEC';
    DELETE FROM doc_template_chapter WHERE template_id = tpl_id;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387 6.1', '规定本规范的主题内容和适用实体(系统级之下技术状态项目)')
    RETURNING id INTO ch1;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.1', '主题内容', 2, 11, TRUE, 'GJB 6387 6.1.1', '明确规范的主题内容');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.2', '实体说明', 2, 12, FALSE, 'GJB 6387 6.1.2', '简要描述技术状态项目在工作分解结构中的层次，列出主要组成');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, FALSE, 'GJB 6387 5.10', '列出规范中引用的所有文件');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387 6.3', '规定技术状态项目必须满足的各项要求(34要素)')
    RETURNING id INTO ch3;

    -- 复制34个要素（与A类相同的结构）
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '功能', 2, 31, TRUE, 'GJB 6387 6.3.1', '规定分系统/设备在系统内与作战使用相关的任务能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', '性能', 2, 32, TRUE, 'GJB 6387 6.3.2', '规定表征实体能力的指标要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.3', '作战适用性', 2, 33, TRUE, 'GJB 6387 6.3.3', '规定实体投入使用的满意程度');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.4', '环境适应性', 2, 34, TRUE, 'GJB 6387 6.3.4', '规定实体在各种环境作用下的适应能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.5', '可靠性', 2, 35, TRUE, 'GJB 6387 6.3.5', '规定实体无故障执行功能的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.6', '维修性', 2, 36, TRUE, 'GJB 6387 6.3.6', '规定实体保持和恢复到规定状态的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.7', '保障性', 2, 37, TRUE, 'GJB 6387 6.3.7', '规定设计特性和保障资源满足战备完好性的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.8', '测试性', 2, 38, TRUE, 'GJB 6387 6.3.8', '规定实体确定状态和隔离故障的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.9', '耐久性', 2, 39, TRUE, 'GJB 6387 6.3.9', '规定实体达到极限状态前完成规定功能的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.10', '安全性', 2, 310, TRUE, 'GJB 6387 6.3.10', '规定实体以可接受风险执行功能的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.11', '信息安全', 2, 311, FALSE, 'GJB 6387 6.3.11', '规定实体在重要系统中的信息安全能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.12', '隐蔽性', 2, 312, FALSE, 'GJB 6387 6.3.12', '规定实体不易被敌方发现跟踪的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.13', '兼容性', 2, 313, TRUE, 'GJB 6387 6.3.13', '规定实体与其他实体互不干扰的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.14', '运输性', 2, 314, FALSE, 'GJB 6387 6.3.14', '规定实体有效转移的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.15', '人机工程', 2, 315, FALSE, 'GJB 6387 6.3.15', '规定实体与人/环境的关系和协调方式');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.16', '互换性', 2, 316, FALSE, 'GJB 6387 6.3.16', '规定实体在尺寸和功能上互相替换的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.17', '稳定性', 2, 317, FALSE, 'GJB 6387 6.3.17', '规定实体控制理化性能变化的能力');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.18', '综合保障', 2, 318, TRUE, 'GJB 6387 6.3.18', '规定实体综合保障要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.19', '接口', 2, 319, TRUE, 'GJB 6387 6.3.19', '规定实体的外部接口和内部接口');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.20', '经济可承受性', 2, 320, FALSE, 'GJB 6387 6.3.20', '规定寿命周期费用的可承受性');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.21', '计算机硬件与软件', 2, 321, FALSE, 'GJB 6387 6.3.21', '规定计算机硬件和软件要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.22', '尺寸和体积', 2, 322, FALSE, 'GJB 6387 6.3.22', '规定外形尺寸和体积限制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.23', '重量', 2, 323, FALSE, 'GJB 6387 6.3.23', '规定重量限制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.24', '颜色', 2, 324, FALSE, 'GJB 6387 6.3.24', '规定颜色限制性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.25', '抗核加固', 2, 325, FALSE, 'GJB 6387 6.3.25', '规定抗核加固要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.26', '理化性能', 2, 326, FALSE, 'GJB 6387 6.3.26', '规定理化性能要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.27', '能耗', 2, 327, FALSE, 'GJB 6387 6.3.27', '规定能源消耗要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.28', '材料', 2, 328, FALSE, 'GJB 6387 6.3.28', '规定所用材料的限制性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.29', '非研制项目', 2, 329, FALSE, 'GJB 6387 6.3.29', '规定采用非研制项目的要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.30', '外观质量', 2, 330, FALSE, 'GJB 6387 6.3.30', '规定外观质量要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.31', '标志和代号', 2, 331, FALSE, 'GJB 6387 6.3.31', '规定标志和代号的要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.32', '主要组成部分特性', 2, 332, FALSE, 'GJB 6387 6.3.32', '规定各主要组成部分的特性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.33', '图样和技术文件', 2, 333, FALSE, 'GJB 6387 6.3.33', '列出生产图样和技术文件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.34', '标准样件', 2, 334, FALSE, 'GJB 6387 6.3.34', '规定标准样件要求');

    -- 第4章 验证
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387 6.4', '规定验证实体是否符合要求的检验要求')
    RETURNING id INTO ch4;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '检验分类', 2, 41, TRUE, 'GJB 6387 6.4.1', '确定检验类别及其组合');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '检验条件', 2, 42, TRUE, 'GJB 6387 6.4.2', '规定检验环境条件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '定型(鉴定)试验', 2, 44, TRUE, 'GJB 6387 6.4.4', '规定定型试验的检验项目、顺序、受检样品数及合格判据');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '质量一致性检验', 2, 46, FALSE, 'GJB 6387 6.4.6', '规定质量一致性检验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.5', '检验方法', 2, 49, TRUE, 'GJB 6387 6.4.11', '规定检验方法');

    -- 第5章
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '包装、运输与贮存', 1, 50, FALSE, 'GJB 6387 6.5', '规定包装运输贮存要求')
    RETURNING id INTO ch5;

    -- 第6章
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '说明事项', 1, 60, FALSE, 'GJB 6387 6.6', '提供说明性信息')
    RETURNING id INTO ch6;

    RAISE NOTICE 'B_SPEC template chapters created for template_id=%', tpl_id;
END $$;

-- ============================================================
-- 4. 材料规范(E类) — GJB 6387 第8章 9要素章节结构
-- ============================================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch3 BIGINT; ch4 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-E-SPEC';
    DELETE FROM doc_template_chapter WHERE template_id = tpl_id;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387 8.1', '规定本规范的主题内容');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, FALSE, 'GJB 6387 8.2', '列出规范中引用的所有文件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387 8.3', '规定材料必须满足的要求')
    RETURNING id INTO ch3;

    -- 材料规范9要素
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '状态特征', 2, 31, TRUE, 'GJB 6387 8.3.1', '材料牌号、名称、成分、供应状态等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', '物理及化学性能', 2, 32, TRUE, 'GJB 6387 8.3.2', '密度、热性能、电性能、磁性能、光学性能、抗氧化性能、耐腐蚀性能、粘度等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.3', '力学性能', 2, 33, TRUE, 'GJB 6387 8.3.3', '硬度、拉伸性能、压缩性能、冲击性能、持久和蠕变性能、弹性性能、断裂性能等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.4', '工艺性能', 2, 34, TRUE, 'GJB 6387 8.3.4', '成形性能、施工性能、适用期等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.5', '环境适应性', 2, 35, TRUE, 'GJB 6387 8.3.5', '耐高低温性能、耐盐雾性能、防霉性能、耐湿热性能等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.6', '组织', 2, 36, FALSE, 'GJB 6387 8.3.6', '高倍组织、低倍组织等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.7', '外形、尺寸及重量', 2, 37, TRUE, 'GJB 6387 8.3.7', '外形、尺寸及重量要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.8', '稳定性', 2, 38, FALSE, 'GJB 6387 8.3.8', '贮存寿命和抗老化要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.9', '毒性', 2, 39, FALSE, 'GJB 6387 8.3.9', '有毒物品的限制性要求和安全防护措施');

    -- 第4章
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387 8.4', '规定验证材料是否符合要求的检验要求')
    RETURNING id INTO ch4;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '检验分类', 2, 41, TRUE, 'GJB 6387 8.4.1', '确定检验类别及其组合');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '检验条件', 2, 42, TRUE, 'GJB 6387 8.4.2', '规定检验环境条件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '定型(鉴定)试验', 2, 43, TRUE, 'GJB 6387 8.4.3', '规定定型试验要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '质量一致性检验', 2, 46, FALSE, 'GJB 6387 8.4.5', '质量一致性检验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.5', '检验方法', 2, 49, TRUE, 'GJB 6387 8.4.10', '规定检验方法');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '包装、运输与贮存', 1, 50, FALSE, 'GJB 6387 8.5', '规定包装运输贮存要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '说明事项', 1, 60, FALSE, 'GJB 6387 8.6', '提供说明性信息');

    RAISE NOTICE 'E_SPEC template chapters created for template_id=%', tpl_id;
END $$;

-- ============================================================
-- 5. 工艺规范(D类) — GJB 6387 第9章 5大类控制要求
-- ============================================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch3 BIGINT; ch3_1 BIGINT; ch3_2 BIGINT; ch4 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-D-SPEC';
    DELETE FROM doc_template_chapter WHERE template_id = tpl_id;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387 9.1', '规定本规范的主题内容');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, FALSE, 'GJB 6387 9.2', '列出规范中引用的所有文件');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387 9.3', '规定工艺必须满足的要求')
    RETURNING id INTO ch3;

    -- 3.1 一般要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '一般要求', 2, 31, TRUE, 'GJB 6387 9.3.1', '规定实体加工的一般要求')
    RETURNING id INTO ch3_1;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_1, '3.1.1', '环境要求', 3, 311, TRUE, 'GJB 6387 9.3.1.1', '施工场地要求、温湿度通风要求、电源/气源/光源要求、环保措施');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_1, '3.1.2', '安全防护要求', 3, 312, TRUE, 'GJB 6387 9.3.1.2', '安全措施、防护设施(防爆/防火/防辐射/防静电/防有害气体)、报警装置');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_1, '3.1.3', '人员要求', 3, 313, TRUE, 'GJB 6387 9.3.1.3', '技术工种培训要求、特种工艺人员资格要求');

    -- 3.2 控制要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', '控制要求', 2, 32, TRUE, 'GJB 6387 9.3.2', '规定各种因素的控制要求')
    RETURNING id INTO ch3_2;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_2, '3.2.1', '工艺材料控制', 3, 321, TRUE, 'GJB 6387 9.3.2.2', '材料性能和关键特性及其公差、有毒及易燃材料限制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_2, '3.2.2', '工艺设备与工艺装备控制', 3, 322, TRUE, 'GJB 6387 9.3.2.3', '设备与装备的性能/关键特性/设计极限、仪器仪表和专用工具');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_2, '3.2.3', '零件控制', 3, 323, TRUE, 'GJB 6387 9.3.2.4', '重要零件完工后表面状态/形位公差/尺寸公差、检验合格后标记');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_2, '3.2.4', '制造控制', 3, 324, TRUE, 'GJB 6387 9.3.2.5', '制造大纲(组织管理/工艺技术/物资供应/后勤保障)、工艺过程参数、制造工序、加工质量');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3_2, '3.2.5', '包装控制', 3, 325, FALSE, 'GJB 6387 9.3.2.6', '包装形式和防护要求、包装标记、防护包装工艺、装箱工艺');

    -- 第4章 验证
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387 9.4', '规定验证工艺要求的检验要求')
    RETURNING id INTO ch4;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '检验分类', 2, 41, TRUE, 'GJB 6387 9.4.1', '确定检验类别及其组合');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '检验条件', 2, 42, TRUE, 'GJB 6387 9.4.2', '规定检验基本条件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '工艺设计评审', 2, 43, TRUE, 'GJB 6387 9.4.3', '工艺设计评审要求(按GJB 1269-1991)');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '完工后检验', 2, 44, TRUE, 'GJB 6387 9.4.4', '完工后成品检验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.5', '检验方法', 2, 45, TRUE, 'GJB 6387 9.4.5', '规定检验方法');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '说明事项', 1, 60, FALSE, 'GJB 6387 9.5', '提供说明性信息');

    RAISE NOTICE 'D_SPEC template chapters created for template_id=%', tpl_id;
END $$;
