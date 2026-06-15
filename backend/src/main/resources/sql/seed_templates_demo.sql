-- ============================================================
-- 模板打样: 产品规范C类 (TPL-C-SPEC) — GJB 6387-2008 Ch6-7
-- 展示"系统能最好识别的模板"应该怎么写
-- 每个章节充分利用: description / writingTips / sampleContent / standardClauseRef
-- ============================================================

DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-C-SPEC';
    IF tpl_id IS NULL THEN RETURN; END IF;

    -- 先清理旧的章节数据（如果存在）
    DELETE FROM doc_template_chapter WHERE template_id = tpl_id;

    -- ============================================
    -- 第1章 范围
    -- ============================================
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, 
        chapter_level, order_num, is_required, standard_clause_ref, 
        description, writing_tips, sample_content) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387-2008 4.2.3.2',
     '本章规定本规范的适用范围和不适用范围。',
     E'撰写要点:
'
      '1. 第一句用"本规范规定了……"的句式，说明规范的主题内容
'
      '2. 第二句用"本规范适用于……"的句式，明确适用对象
'
      '3. 第三句(可选)用"本规范不适用于……"排除不需要覆盖的内容
'
      '4. 总字数控制在100字以内
'
      '5. 适用对象必须包含产品名称(或产品代号)和产品类型',
     E'示例:
'
      '本规范规定了XXX型卫星导航抗干扰装置的的要求、质量保证规定和交货准备等内容。
'
      '本规范适用于XXX型卫星导航抗干扰装置的制造、检验和验收。') RETURNING id INTO ch1;

    -- ============================================
    -- 第2章 引用文件
    -- ============================================
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 6387-2008 4.2.3.2',
     '列出本规范正文中引用的所有标准、规范和其他文件。',
     E'撰写要点:
'
      '1. 开头必须写引导句:"下列文件中的条款通过本规范的引用而成为本规范的条款。'
         '凡是注日期的引用文件，其随后所有的修改单(不包括勘误的内容)或修订版均不适用于本规范。'
         '凡是不注日期的引用文件，其最新版本适用于本规范。"
'
      '2. 每条引用格式为:"GJB XXX-YYYY 标准名称"
'
      '3. 按标准代号字母顺序排列(GJB→GJB/Z→GB→GB/T→SJ→...)
'
      '4. 只列正文中实际引用到的文件，不列参考性文献',
     E'示例:
'
      '下列文件中的条款通过本规范的引用而成为本规范的条款。凡是注日期的引用文件，'
      '其随后所有的修改单(不包括勘误的内容)或修订版均不适用于本规范。'
      '凡是不注日期的引用文件，其最新版本适用于本规范。

'
      'GJB 150.3A-2009 军用装备实验室环境试验方法 第3部分:高温试验
'
      'GJB 151B-2013 军用设备和分系统电磁发射和敏感度要求与测量
'
      'GJB 152B-2013 军用设备和分系统电磁发射和敏感度测量
'
      'GJB 179A-1996 计数抽样检验程序及表
'
      'GJB 450A-2004 装备可靠性工作通用要求
'
      'GJB 6387-2008 武器装备研制项目专用规范编写规定
'
      'GJB 900A-2012 装备安全性工作通用要求') RETURNING id INTO ch2;

    -- ============================================
    -- 第3章 要求 (核心章 — 分11个子节)
    -- ============================================
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387-2008 4.2.3.2',
     '规定产品必须满足的全部要求。这是产品规范的核心章节，所有要求必须是可检验、可度量的。',
     E'关键规则:
'
      '1. 使用"应"字句表达要求型条款，不用"必须"或"需要"
'
      '2. 每个要求都必须是可验证的，避免模糊表述如"良好""适当"
'
      '3. 指标要求必须有具体数值和允差，如"输出电压应为28V±0.5V"
'
      '4. 接口要求必须精确到连接器引脚号、信号名称和电气参数
'
      '5. 物理特性必须标注尺寸公差和材料牌号
'
      '6. 以下各子节按照重要性递减排列，必写子节不可跳过') RETURNING id INTO ch3;

    -- 3.1 产品概述
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.1', '产品概述', 2, 31, TRUE, 'GJB 6387-2008 3.1.3',
     '描述产品的组成、工作原理、主要功能和使用场景。',
     E'撰写要点:
'
      '1. 用一段话概括产品的使命:"XXX装置是……，主要用于……"
'
      '2. 列出产品的主要组成部分(模块清单)，每项用一句话说明功能
'
      '3. 简述工作原理(信号流/数据流/控制流)
'
      '4. 说明产品的主要工作模式和切换方式
'
      '5. 字数控制在300-500字',
     E'示例:
'
      'XXX装置是安装在制导控制尾舱内的组合导航设备，集卫星导航定位、'
      '抗压制干扰和反欺骗干扰功能于一体，为制导控制系统提供精确的位置、'
      '速度和时间信息。

'
      '该装置由以下主要功能单元组成:
'
      'a) 抗干扰天线阵:包含XXX个天线阵元及射频前端处理电路;
'
      'b) 接收机模块:包含射频通道、基带信号处理和导航定位解算单元;
'
      'c) 电源模块:提供二次电源转换及滤波功能;
'
      'd) 接口模块:提供与弹载计算机的通信通道。');

    -- 3.2 功能特性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.2', '功能特性', 2, 32, TRUE, 'GJB 6387-2008 3.4.1',
     '逐项列出产品应具备的全部功能和性能指标。这是第3章最重要的子节。',
     E'撰写要点:
'
      '1. 每个功能用一个独立的条目(a/b/c...)表述
'
      '2. 每条目包含:功能名称 + 功能描述 + 量化性能指标
'
      '3. 性能指标必须包含:参数名称、数值范围、单位、测试条件(如适用)
'
      '4. 动态指标标注响应时间、更新率等时效参数
'
      '5. 精度指标必须注明是在什么条件下(如"在信号电平≥-130dBm时")
'
      '6. 项目主数据中已有的参数直接填入，缺失的保留XXX占位符',
     E'示例:
'
      'a) 卫星导航定位功能
'
      '  产品应具备BDS B1I/B3I和GPS L1频点的信号接收与定位解算能力。
'
      '  1) 定位精度(1σ):水平≤XXX m，高程≤XXX m(在信号电平≥-130dBm时);
'
      '  2) 测速精度(1σ):≤XXX m/s;
'
      '  3) 首次定位时间(TTFF):冷启动≤XXX s，热启动≤XXX s;
'
      '  4) 数据更新率:1/2/5/10 Hz可配置。

'
      'b) 抗压制式干扰功能
'
      '  产品应能对抗来自XXX方向的压制式干扰信号。
'
      '  1) 抗干扰数量:不少于XXX个方向;
'
      '  2) 最大干信比(J/S):≥XXX dB;
'
      '  3) 干扰抑制能力:在XXX dB干信比条件下，定位精度不劣于XXX m。');

    -- 3.3 物理特性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.3', '物理特性', 2, 33, TRUE, 'GJB 6387-2008 3.4.2',
     '规定产品的外形尺寸、重量、颜色、标识和材料等物理属性。',
     E'撰写要点:
'
      '1. 外形尺寸必须标注三维尺寸(长×宽×高)及公差
'
      '2. 重量标注最大值:"质量应不大于XXX kg"
'
      '3. 颜色标注标准色号或实物样品编号
'
      '4. 标识要求包括:产品铭牌内容、位置、字体高度、耐久性要求
'
      '5. 壳体材料标注材料牌号(如铝合金2A12-T4)和表面处理方式',
     E'示例:
'
      'a) 外形尺寸
'
      '  产品外形尺寸应不大于XXX mm × XXX mm × XXX mm(不含连接器和天线罩)。

'
      'b) 重量
'
      '  产品总质量应不大于XXX kg。

'
      'c) 颜色与标识
'
      '  产品壳体颜色应为XXX色(色号XXX)。
'
      '  产品应在壳体显著位置设置铭牌，铭牌内容应至少包括:
'
      '  1) 产品名称和型号;
'
      '  2) 产品代号;
'
      '  3) 出厂编号;
'
      '  4) 生产日期;
'
      '  5) 承制单位名称。
'
      '  铭牌文字高度不小于2.5mm，应采用激光刻印，保证全寿命周期内清晰可读。');

    -- 3.4 接口要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.4', '接口要求', 2, 34, TRUE, 'GJB 6387-2008 3.4.3',
     '规定产品的所有外部接口，包括机械接口、电气接口和数据接口。',
     E'撰写要点:
'
      '1. 机械接口:安装孔位/螺孔尺寸/连接器型号和位置
'
      '2. 电气接口:每个连接器的引脚定义表(引脚号/信号名称/类型/方向/电平范围)
'
      '3. 数据接口:通信协议/波特率/数据帧格式/ICD引用
'
      '4. 射频接口:阻抗/驻波比/频率范围/最大输入功率
'
      '5. 接口定义必须精确到能在生产线上直接使用的程度',
     E'示例:
'
      'a) 电源接口
'
      '  采用J30J-XXTJ连接器，引脚定义如下:
'
      '  | 引脚号 | 信号名称 | 类型 | 方向 | 电平/范围 |
'
      '  |--------|----------|------|------|-------|
'
      '  | 1 | +28V | 电源 | 输入 | +28V±3V |
'
      '  | 2 | +28V_RTN | 电源 | 输入 | 0V |
'
      '  | 3 | +5V_STBY | 电源 | 输入 | +5V±0.25V |

'
      'b) 通信接口
'
      '  采用J30J-XXZK连接器，通信方式为RS422异步串行。
'
      '  1) 波特率:115200bps;
'
      '  2) 数据格式:1位起始位 8位数据位 1位停止位 无校验;
'
      '  3) 数据帧格式和消息定义见XXX-ICD-001。');

    -- 3.5 环境适应性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.5', '环境适应性', 2, 35, TRUE, 'GJB 150.1A-2009',
     '规定产品应满足的自然环境和诱导环境条件。',
     E'撰写要点:
'
      '1. 按GJB 150的方法逐项列出环境条件
'
      '2. 每项标注:条件名称、参数范围、持续时间和GJB 150对应部分编号
'
      '3. 至少覆盖:高温/低温/温度冲击/振动/冲击/湿热/盐雾/低气压
'
      '4. 振动条件应给出频谱图和加速度量级
'
      '5. 温度条件标注工作温度和贮存温度范围',
     E'示例:
'
      'a) 高温工作
'
      '  产品在+70℃环境温度下应正常工作，试验方法按GJB 150.3A。

'
      'b) 低温工作
'
      '  产品在-40℃环境温度下应正常启动和工作，试验方法按GJB 150.4A。

'
      'c) 温度冲击
'
      '  产品在-55℃～+85℃温度范围内经历XXX次温度循环后，'
      '应无机械损伤和电气性能下降，试验方法按GJB 150.5A。

'
      'd) 振动
'
      '  产品应能承受如下随机振动条件(按GJB 150.16A):
'
      '  | 频率(Hz) | 功率谱密度(g²/Hz) |
'
      '  |----------|-------------------|
'
      '  | 20-XXX | XXX |
'
      '  | XXX-2000 | XXX |
'
      '  振动持续时间:每轴向XXX min。');

    -- 3.6 电磁兼容性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.6', '电磁兼容性', 2, 36, TRUE, 'GJB 151B-2013',
     '规定产品的电磁发射限值和电磁敏感度要求。',
     E'撰写要点:
'
      '1. 按GJB 151B列出发射和敏感度项目清单
'
      '2. 每项标注:项目代号(如CE102/RE102/CS114/RS103)、适用频率范围、限值
'
      '3. 关键项目必须标注限值曲线或具体dB值
'
      '4. 注明哪些项目是必测、哪些是选测',
     E'示例:
'
      '产品的电磁兼容性应符合GJB 151B-2013的要求，具体项目和限值如下:
'
      '| 项目代号 | 项目名称 | 频率范围 | 限值要求 | 备注 |
'
      '|----------|----------|----------|----------|------|
'
      '| CE102 | 电源线传导发射 | 10kHz-10MHz | 图CE102-1 | 必测 |
'
      '| RE102 | 电场辐射发射 | 2MHz-18GHz | 图RE102-3 | 必测 |
'
      '| CS114 | 电缆束注入传导敏感度 | 10kHz-400MHz | 曲线5 | XXX |
'
      '| RS103 | 电场辐射敏感度 | 2MHz-40GHz | 200V/m | XXX |');

    -- 3.7 可靠性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.7', '可靠性', 2, 37, TRUE, 'GJB 450A-2004',
     '规定产品的可靠性定量要求和定性要求。',
     E'撰写要点:
'
      '1. 量化可靠性指标:MTBF或可靠度，附置信水平
'
      '2. 标注可靠性试验条件和统计方案引用
'
      '3. 对关键功能可单独提可靠性要求
'
      '4. 使用"应在XXX条件下MTBF不小于XXX小时(置信度XXX%)"的句式',
     E'示例:
'
      'a) 平均故障间隔时间(MTBF)
'
      '  产品在综合环境条件下的MTBF应不低于XXX小时(置信度80%)，'
      '可靠性鉴定试验方法按GJB 899A。

'
      'b) 任务可靠度
'
      '  产品在XXX分钟典型任务剖面内的任务可靠度应不低于XXX。');

    -- 3.8 维修性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.8', '维修性', 2, 38, TRUE, 'GJB 368B-2009',
     '规定产品的维修性定量定性要求。',
     E'撰写要点:
'
      '1. 量化:平均修复时间(MTTR)或最大修复时间
'
      '2. 定性:可达性/标准化/模块化/防差错设计
'
      '3. 注明维修级别(基层级/中继级/基地级)',
     E'示例:
'
      'a) 平均修复时间(MTTR)
'
      '  在基层级维修条件下，产品的平均修复时间应不超过XXX分钟。

'
      'b) 维修可达性
'
      '  各功能模块应具备独立的拆装路径，更换任一模块无需拆卸其他模块。');

    -- 3.9 安全性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.9', '安全性', 2, 39, TRUE, 'GJB 900A-2012',
     '规定产品的安全性设计和验证要求。',
     E'撰写要点:
'
      '1. 列出I级和II级危险的清单及控制措施
'
      '2. 电源安全:过流保护/反接保护/短路保护
'
      '3. 弹药安全:自毁信号隔离要求(如适用)
'
      '4. 电磁辐射安全:对人员和军械的辐射安全限值',
     E'示例:
'
      'a) 电气安全
'
      '  1) 电源输入端应具备防反接保护，反接情况下产品不应损坏;
'
      '  2) 电源输入端应具备过流保护，输入电流超过XXX A时自动切断。

'
      'b) 电磁辐射安全
'
      '  产品正常工作时，天线口面的辐射功率密度应不超过XXX mW/cm²。');

    -- 3.10 制造要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.10', '制造要求', 2, 310, TRUE, 'GJB 6387-2008 3.4.4',
     '规定产品制造过程中的关键工序、特殊过程和检验要求。',
     E'撰写要点:
'
      '1. 列出关键工序清单和关键参数控制要求
'
      '2. 列出特殊过程(如焊接/灌封/三防处理)及其控制标准
'
      '3. 规定首件鉴定和关键件控制要求
'
      '4. 车间洁净度/防静电/温湿度等环境要求',
     E'示例:
'
      'a) 关键工序
'
      '  以下工序应列为关键工序，实施过程能力控制和100%检验:
'
      '  1) 射频模块的贴片焊接:采用XXX工艺，焊点检验按IPC-A-610 XXX级;
'
      '  2) 天线阵元的组装与相位校准:相位一致性应≤XXX°。

'
      'b) 特殊过程
'
      '  1) 模块灌封:采用XXX型灌封胶，按XXX工艺规程执行;
'
      '  2) 三防涂覆:所有印制板组件应进行三防涂覆，按GJB 1463执行。');

    -- 3.11 标识和可追溯性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch3, '3.11', '标识和可追溯性', 2, 311, TRUE, 'GJB 726A-2004',
     '规定产品的标识方法、检验状态标识和可追溯性要求。',
     E'撰写要点:
'
      '1. 标识范围:产品/部件/关键件/重要件的标识方式
'
      '2. 标识方法:铭牌/激光刻印/条码/RFID等
'
      '3. 可追溯性:追溯链的关键节点和记录要求
'
      '4. 检验状态标识:待检/合格/不合格/待处理的状态标识方式',
     E'示例:
'
      'a) 产品标识
'
      '  每套产品应在壳体显著位置设置永久性标识，标识应至少包含产品代号、'
      '出厂编号和承制单位代号。标识应能耐受规定的环境试验条件而不褪色、不脱落。

'
      'b) 可追溯性
'
      '  关键件和重要件应建立从原材料入厂到成品交付的完整追溯链，追溯记录应包括:
'
      '  1) 原材料/元器件批次号;
'
      '  2) 关键工序的操作者和检验者;
'
      '  3) 环境应力筛选结果;
'
      '  4) 最终检验数据。');

    -- ============================================
    -- 第4章 验证
    -- ============================================
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387-2008 4.2.3.2',
     '规定验证第3章各项要求的方法、检验分类和合格判据。每一要求条款都应有对应的验证方法。',
     E'撰写要点:
'
      '1. 验证方法分为四类:分析/演示/试验/检查
'
      '2. 检验分为:鉴定检验(定型阶段)和质量一致性检验(批产阶段)
'
      '3. 必须提供验证矩阵(要求条款→验证方法→检验类别→合格判据的对应表)
'
      '4. 抽样方案引用GJB 179A') RETURNING id INTO ch4;

    -- 4.1 检验分类
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch4, '4.1', '检验分类', 2, 41, TRUE, 'GJB 6387-2008',
     '规定产品的检验分类:鉴定检验和质量一致性检验。',
     E'撰写要点:
'
      '1. 鉴定检验:说明何时进行(设计定型/工艺变更/转产时)
'
      '2. 质量一致性检验:分为A组(逐件)/B组(抽样)/C组(周期)/D组(必要时)
'
      '3. 说明各组检验的适用条件、频次和覆盖范围',
     E'示例:
'
      'a) 鉴定检验
'
      '  产品在设计定型阶段应进行鉴定检验，检验项目覆盖第3章全部要求。
'
      '  在发生以下情况之一时应重新进行鉴定检验:
'
      '  1) 产品设计、工艺或材料有重大更改;
'
      '  2) 停产超过XXX年后恢复生产;
'
      '  3) 上级部门要求。

'
      'b) 质量一致性检验
'
      '  A组检验(逐件):每套产品均应进行的检验项目，主要包括外观检查、
'
      '  常温功能性能检验和ESS筛选。
'
      '  B组检验(抽样):按GJB 179A进行抽样，主要包括环境试验、EMC试验等。
'
      '  C组检验(周期):每XXX批或每XXX年进行一次，主要包括可靠性试验。');

    -- 4.2 检验方法
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch4, '4.2', '检验方法', 2, 42, TRUE, 'GJB 6387-2008',
     '详细描述各项检验的操作步骤、使用设备和测量方法。',
     E'撰写要点:
'
      '1. 对每一项检验，描述:目的→设备→步骤→数据处理→判定
'
      '2. 测量设备应标注型号、量程、精度和校准要求
'
      '3. 复杂试验(如EMC)可引用对应的GJB试验方法标准
'
      '4. 功能检验可引用专用的自动测试设备(ATE)',
     E'示例:
'
      'a) 导航定位精度检验
'
      '  1) 目的:验证3.2a)条规定的定位精度要求。
'
      '  2) 设备:GNSS信号模拟器(型号XXX，精度XXX)。
'
      '  3) 步骤:将产品通过射频电缆连接至信号模拟器，设置模拟场景为XXX，'
      '记录产品输出的定位数据，连续采集XXX个样本点。
'
      '  4) 数据处理:计算所有样本点与真实位置的偏差，统计1σ值。
'
      '  5) 判定:水平定位1σ值≤XXX m且垂直定位1σ值≤XXX m为合格。');

    -- 4.3 验证矩阵
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch4, '4.3', '验证矩阵', 2, 43, TRUE, 'GJB 6387-2008',
     '以表格形式列出第3章每项要求对应的验证方法、检验类别和合格判据。',
     E'撰写要点:
'
      '1. 表格列:序号/要求条款号/要求内容摘要/验证方法(分析/演示/试验/检查)/检验类别(鉴定/A组/B组/C组)/合格判据
'
      '2. 确保第3章的每一项要求都在矩阵中有对应的验证条目
'
      '3. 检验类别用缩写:Q=鉴定检验 A=A组 B=B组 C=C组',
     E'示例:
'
      '| 序号 | 条款 | 要求摘要 | 验证方法 | 检验类别 | 合格判据 |
'
      '|------|------|----------|----------|----------|-------|
'
      '| 1 | 3.2a) | 定位精度 | 试验 | Q,B | 水平1σ≤XXX m |
'
      '| 2 | 3.2b) | 抗干扰能力 | 试验 | Q | J/S≥XXX dB时定位正常 |
'
      '| 3 | 3.3a) | 外形尺寸 | 检查 | Q,A | 不超过XXX×XXX×XXX mm |
'
      '| 4 | 3.5a) | 高温工作 | 试验 | Q,B | +70℃正常工作XXX h |
'
      '| 5 | 3.6 | 电磁兼容性 | 试验 | Q | 满足GJB 151B限值 |');

    -- ============================================
    -- 第5章 包装、运输与贮存
    -- ============================================
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips) VALUES
    (tpl_id, 0, '5', '包装、运输与贮存', 1, 50, TRUE, 'GJB 6387-2008 4.2.3.2',
     '规定产品的防护包装、装箱、运输和贮存要求。',
     E'撰写要点:
'
      '1. 包装分等级:按GJB 145A规定包装等级
'
      '2. 运输:标注运输方式(公路/铁路/空运/海运)和运输过程中的环境极限
'
      '3. 贮存:标注贮存环境条件(温度/湿度/洁净度)、贮存期限和定期维护要求
'
      '4. 如有特殊要求(如静电防护/防辐射/防磁)单独说明') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch5, '5.1', '防护包装', 2, 51, TRUE, 'GJB 145A',
     '产品的防护包装等级、包装材料和包装方法。',
     E'包装等级用GJB 145A的定义:A级=最高防护 C级=最低防护',
     E'示例:
'
      '产品采用A级防护包装，包装应满足以下要求:
'
      'a) 产品先用防静电袋密封包装，内放干燥剂(硅胶，XXX g);
'
      'b) 装入定制的EVA缓冲衬垫内，衬垫厚度不小于XXX mm;
'
      'c) 外层采用XXX型军用包装箱，箱体应能承受XXX kg的堆码压力。');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch5, '5.2', '运输要求', 2, 52, TRUE, 'GJB 6387',
     '产品在运输过程中的防护要求和允许的环境条件。',
     E'标注运输振动和冲击的量级，以及与包装等级的对应关系',
     E'示例:
'
      '产品经防护包装后应能适应以下运输条件:
'
      'a) 运输方式:公路、铁路、空运;
'
      'b) 运输温度:-40℃～+70℃;
'
      'c) 运输振动:按GJB 150.16A公路运输谱，持续时间XXX h;
'
      'd) 运输冲击:半正弦波 XXX g / XXX ms。');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, ch5, '5.3', '贮存要求', 2, 53, TRUE, 'GJB 6387',
     '产品在仓库贮存的环境条件、贮存期限和定期维护要求。',
     E'标注有包装和无包装两种贮存条件的差异',
     E'示例:
'
      '产品经防护包装后在仓库内的贮存应满足:
'
      'a) 贮存环境温度:5℃～35℃;
'
      'b) 相对湿度:≤75%(25℃时);
'
      'c) 库房应清洁、干燥、通风，无腐蚀性气体;
'
      'd) 贮存期限:自出厂之日起不少于XXX年;
'
      'e) 超过XXX年的产品应开箱检查并重新进行A组检验合格后方可交付。');

    -- ============================================
    -- 第6章 说明事项
    -- ============================================
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title,
        chapter_level, order_num, is_required, standard_clause_ref,
        description, writing_tips, sample_content) VALUES
    (tpl_id, 0, '6', '说明事项', 1, 60, TRUE, 'GJB 6387-2008 4.2.3.2',
     '提供规范的附加信息:预定用途、术语定义、订购文件中应明确的内容。',
     E'撰写要点:
'
      '1. 预定用途:说明产品的使命任务和作战使用环境
'
      '2. 术语定义(如需要):为第3章使用的专有术语下定义
'
      '3. 订购文件中应明确的内容:列出需要订购方在合同中明确的选项(如天线长度/连接器类型)
'
      '4. 不强制要求的内容不必写',
     E'示例:
'
      '6.1 预定用途
'
      '本规范规定的XXX装置预定用于XXX型武器系统，在XXX条件下为制导控制系统'
      '提供导航定位信息。

'
      '6.2 订购文件中应明确的内容
'
      '订购方在合同中应至少明确以下内容:
'
      'a) 产品代号和名称;
'
      'b) 订购数量;
'
      'c) 交付进度;
'
      'd) 是否需要提供专用测试设备;
'
      'e) 备件数量和要求;
'
      'f) 技术资料交付要求;
'
      'g) 培训要求。') RETURNING id INTO ch6;

    RAISE NOTICE 'TPL-C-SPEC 模板打样完成: 6章, % 个子节覆盖全部GJB 6387六章固定格式';
END $$;
