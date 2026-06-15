-- ============================================================
-- V2: 可靠性设计模块数据表
-- GJB/Z 299D-2024 电子设备可靠性预计手册 配套
-- ============================================================

-- 1. 可靠性指标要求
CREATE TABLE IF NOT EXISTS rel_requirement (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    -- 基本可靠性指标
    mtbf_hours DOUBLE PRECISION,              -- MTBF 平均故障间隔时间(h)
    mtbcf_hours DOUBLE PRECISION,             -- MTBCF 平均致命故障间隔时间(h)
    -- 任务可靠性
    reliability_at_time DOUBLE PRECISION,      -- R(t) 可靠度值 (如 0.99)
    reliability_time_hours DOUBLE PRECISION,   -- R(t) 对应的工作时间
    -- 寿命
    service_life_years DOUBLE PRECISION,       -- 使用寿命(年)
    storage_reliability DOUBLE PRECISION,      -- 贮存可靠度
    -- 判据与方法
    failure_criteria TEXT,                     -- 故障判据
    verification_method VARCHAR(50) DEFAULT 'ANALYSIS',  -- TEST/ANALYSIS/EVALUATION
    requirement_source VARCHAR(50) DEFAULT 'CONTRACT',   -- CONTRACT/SPEC/ALLOCATION
    -- 审计
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. 可靠性预计记录（主表）
CREATE TABLE IF NOT EXISTS rel_prediction (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    prediction_method VARCHAR(30) NOT NULL DEFAULT 'STRESS',
        -- STRESS: 元器件应力分析法
        -- COUNT: 元器件计数法
        -- SIMILAR: 相似产品法
    environment_category VARCHAR(20),          -- G_FIX/A_IF/A_UF/G_M1/N_S2...
    total_failure_rate DOUBLE PRECISION,       -- λ_total (10⁻⁶/h)
    mtbf_result DOUBLE PRECISION,              -- MTBF 计算结果(h)
    is_compliant BOOLEAN,                      -- 是否满足 rel_requirement 要求
    target_mtbf DOUBLE PRECISION,              -- 对照的目标 MTBF
    notes TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. 预计明细（器件级）
CREATE TABLE IF NOT EXISTS rel_prediction_item (
    id BIGSERIAL PRIMARY KEY,
    prediction_id BIGINT NOT NULL REFERENCES rel_prediction(id) ON DELETE CASCADE,
    part_name VARCHAR(200) NOT NULL,           -- 器件名称/位号
    part_category VARCHAR(50) NOT NULL,        -- 大类: 微电路/分立器件/电阻/电容/电感/继电器...
    part_subtype VARCHAR(100),                 -- 子类型: 单片IC/混合IC/碳膜电阻...
    part_spec VARCHAR(200),                    -- 规格型号
    quantity INTEGER NOT NULL DEFAULT 1,       -- 数量
    quality_level VARCHAR(20),                 -- 质量等级: A1/A2/B1/B2/C
    -- 基本失效率
    lambda_b DOUBLE PRECISION,                 -- 基本失效率 λ_b (10⁻⁶/h)
    -- π 系数
    pi_e DOUBLE PRECISION,                     -- 环境系数
    pi_q DOUBLE PRECISION,                     -- 质量系数
    pi_t DOUBLE PRECISION,                     -- 温度应力系数
    pi_s DOUBLE PRECISION,                     -- 电应力系数
    pi_l DOUBLE PRECISION,                     -- 复杂度系数(IC类)
    pi_c DOUBLE PRECISION,                     -- 封装复杂度(混合电路)
    pi_a DOUBLE PRECISION,                     -- 应用系数(MMIC)
    pi_k DOUBLE PRECISION,                     -- 种类系数
    pi_cvc DOUBLE PRECISION,                   -- 非易失存储器编程工艺系数
    -- 计算结果
    lambda_p DOUBLE PRECISION,                 -- 工作失效率 λ_p (10⁻⁶/h)
    -- 工作条件
    operating_temp DOUBLE PRECISION,           -- 工作温度(℃)
    stress_ratio DOUBLE PRECISION,             -- 电应力比 S
    table_ref VARCHAR(50),                     -- 引用的299D表号
    order_num INTEGER NOT NULL DEFAULT 0
);

-- 4. GJB/Z 299D-2024 标准数据缓存（种子数据，支持快速查表）
CREATE TABLE IF NOT EXISTS rel_gjb299d_cache (
    id BIGSERIAL PRIMARY KEY,
    part_category VARCHAR(50) NOT NULL,        -- 器件大类
    part_subtype VARCHAR(100),                 -- 器件子类
    section_ref VARCHAR(20),                   -- 标准章节号(如 5.1.2)
    param_name VARCHAR(50) NOT NULL,           -- 参数名: lambda_b/pi_E/pi_Q/pi_T/C1/C2...
    key_values JSONB NOT NULL DEFAULT '{}',    -- 查表键: {"T":25,"S":0.5,"NG":1000}
    result_value DOUBLE PRECISION NOT NULL,    -- 查表结果值
    table_ref VARCHAR(50),                     -- 标准表号(如 表25)
    table_row_index INTEGER,                   -- 在表中的行位置
    notes TEXT,
    UNIQUE(part_category, part_subtype, param_name, key_values)
);

CREATE INDEX IF NOT EXISTS idx_cache_lookup 
    ON rel_gjb299d_cache(part_category, part_subtype, param_name);

-- 5. 可靠性分配记录
CREATE TABLE IF NOT EXISTS rel_allocation (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    allocation_method VARCHAR(30) NOT NULL,    -- EQUAL/SCORING/AGREE/ARINC/PROPORTIONAL
    -- 目标
    system_mtbf DOUBLE PRECISION,              -- 系统级 MTBF 目标
    system_lambda DOUBLE PRECISION,            -- 系统级 λ (10⁻⁶/h)
    -- 分配对象
    unit_name VARCHAR(200),                    -- 分配单元名称
    unit_level VARCHAR(50),                    -- 层级: SYSTEM/SUBSYSTEM/EQUIPMENT/LRU/SRU
    parent_id BIGINT REFERENCES rel_allocation(id), -- 父级分配记录
    -- 分配参数(按方法不同)
    complexity_score DOUBLE PRECISION,         -- 评分分配法: 复杂度评分
    maturity_score DOUBLE PRECISION,           -- 评分分配法: 技术成熟度评分
    duty_time_score DOUBLE PRECISION,          -- 评分分配法: 工作时间评分
    environment_score DOUBLE PRECISION,        -- 评分分配法: 环境严酷度评分
    importance_factor DOUBLE PRECISION,        -- AGREE法: 重要度
    complexity_factor DOUBLE PRECISION,        -- AGREE法: 复杂度
    existing_lambda DOUBLE PRECISION,          -- ARINC/比例法: 已有失效率
    -- 结果
    allocated_lambda DOUBLE PRECISION,         -- 分配的 λ
    allocated_mtbf DOUBLE PRECISION,           -- 分配的 MTBF
    allocation_ratio DOUBLE PRECISION,         -- 分配比例
    is_verified BOOLEAN DEFAULT FALSE,         -- 分配合理性验证
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_rel_req_project ON rel_requirement(project_id);
CREATE INDEX IF NOT EXISTS idx_rel_pred_project ON rel_prediction(project_id);
CREATE INDEX IF NOT EXISTS idx_rel_pred_item_pred ON rel_prediction_item(prediction_id);
CREATE INDEX IF NOT EXISTS idx_rel_alloc_project ON rel_allocation(project_id);
CREATE INDEX IF NOT EXISTS idx_rel_alloc_parent ON rel_allocation(parent_id);
