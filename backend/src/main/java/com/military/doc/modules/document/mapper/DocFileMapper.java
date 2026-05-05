package com.military.doc.modules.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.military.doc.modules.document.entity.DocFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocFileMapper extends BaseMapper<DocFile> {
}