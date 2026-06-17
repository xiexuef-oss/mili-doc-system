-- Migration v11: Fix master data field names to align AI output with frontend display
-- Issue: AI buildSystemPrompt() outputs one set of field names, frontend displays another
-- This migration normalizes existing data to the frontend-compatible field names

-- Fix tacticalIndicators: value->required, requirementSource->conclusion
UPDATE project_master_data
SET tactical_indicators = (
  SELECT jsonb_agg(
    (elem - 'value' - 'requirementSource' - 'requirementValue' - 'measuredValue' - 'testMethod') ||
    jsonb_build_object(
      'required', COALESCE(elem->>'value', elem->>'requirementValue', ''),
      'actual', COALESCE(elem->>'measuredValue', ''),
      'conclusion', COALESCE(elem->>'requirementSource', ''),
      'remark', '',
      'indicatorName', COALESCE(elem->>'indicatorName', ''),
      'unit', COALESCE(elem->>'unit', '')
    )
  )
  FROM jsonb_array_elements(tactical_indicators) AS elem
)
WHERE tactical_indicators IS NOT NULL
  AND tactical_indicators::text <> '[]'
  AND (tactical_indicators::text LIKE '%"value"%'
       OR tactical_indicators::text LIKE '%"requirementValue"%'
       OR tactical_indicators::text LIKE '%"requirementSource"%');

-- Fix productTree: itemName->productName, itemCode->productCode, parentCode->parentProduct
-- Also convert numeric level to Chinese string
UPDATE project_master_data
SET product_tree = (
  SELECT jsonb_agg(
    (elem - 'itemName' - 'itemCode' - 'parentCode' - 'nodeName' - 'nodeCode' - 'parentNodeCode' - 'nodeLevel') ||
    jsonb_build_object(
      'productName', COALESCE(elem->>'itemName', elem->>'nodeName', ''),
      'productCode', COALESCE(elem->>'itemCode', elem->>'nodeCode', ''),
      'parentProduct', COALESCE(elem->>'parentCode', elem->>'parentNodeCode', ''),
      'level', CASE
        WHEN elem->>'nodeLevel' IS NOT NULL THEN elem->>'nodeLevel'
        WHEN elem->>'level' ~ '^[0-9]+$' AND (elem->>'level')::int = 1 THEN '系统'
        WHEN elem->>'level' ~ '^[0-9]+$' AND (elem->>'level')::int = 2 THEN '分系统'
        WHEN elem->>'level' ~ '^[0-9]+$' AND (elem->>'level')::int = 3 THEN '设备'
        WHEN elem->>'level' ~ '^[0-9]+$' AND (elem->>'level')::int = 4 THEN '组件'
        ELSE COALESCE(elem->>'level', '')
      END,
      'quantity', COALESCE(elem->>'quantity', ''),
      'remark', COALESCE(elem->>'remark', '')
    )
  )
  FROM jsonb_array_elements(product_tree) AS elem
)
WHERE product_tree IS NOT NULL
  AND product_tree::text <> '[]'
  AND (product_tree::text LIKE '%"itemName"%'
       OR product_tree::text LIKE '%"nodeName"%'
       OR product_tree::text LIKE '%"parentCode"%'
       OR product_tree::text ~ '"level":\s*[0-9]');

-- Fix teamMembers: unit->department, contact->phone
UPDATE project_master_data
SET team_members = (
  SELECT jsonb_agg(
    (elem - 'unit' - 'contact' - 'organization' - 'contactPhone' - 'duties') ||
    jsonb_build_object(
      'department', COALESCE(elem->>'unit', elem->>'organization', ''),
      'phone', COALESCE(elem->>'contact', elem->>'contactPhone', ''),
      'email', COALESCE(elem->>'duties', ''),
      'name', COALESCE(elem->>'name', ''),
      'role', COALESCE(elem->>'role', '')
    )
  )
  FROM jsonb_array_elements(team_members) AS elem
)
WHERE team_members IS NOT NULL
  AND team_members::text <> '[]'
  AND (team_members::text LIKE '%"unit"%'
       OR team_members::text LIKE '%"contact"%'
       OR team_members::text LIKE '%"organization"%');

-- Fix milestones: deadline->plannedDate, deliverable->keyDeliverables
UPDATE project_master_data
SET milestones = (
  SELECT jsonb_agg(
    (elem - 'deadline' - 'deliverable' - 'deliverables' - 'milestoneName' - 'acceptanceCriteria') ||
    jsonb_build_object(
      'plannedDate', COALESCE(elem->>'deadline', ''),
      'actualDate', COALESCE(elem->>'actualDate', ''),
      'keyDeliverables', COALESCE(elem->>'deliverable', elem->>'deliverables', elem->>'acceptanceCriteria', ''),
      'name', COALESCE(elem->>'milestoneName', elem->>'name', ''),
      'status', COALESCE(elem->>'status', ''),
      'stageCode', COALESCE(elem->>'stageCode', '')
    )
  )
  FROM jsonb_array_elements(milestones) AS elem
)
WHERE milestones IS NOT NULL
  AND milestones::text <> '[]'
  AND (milestones::text LIKE '%"deadline"%'
       OR milestones::text LIKE '%"deliverable"%'
       OR milestones::text LIKE '%"milestoneName"%');
