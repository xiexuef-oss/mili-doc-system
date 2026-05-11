package com.military.doc.modules.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.military.doc.modules.document.entity.DocLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocLedgerMapper extends BaseMapper<DocLedger> {

    @Select("SELECT * FROM doc_ledger WHERE project_id=#{projectId} AND stage_id=#{stageId} AND lifecycle_status NOT IN ('RELEASED','ARCHIVED') AND deleted=0")
    List<DocLedger> findUnreleasedByStage(@Param("projectId") Long projectId, @Param("stageId") Long stageId);
}
