package com.military.doc.modules.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.military.doc.modules.document.entity.DocEditLock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocEditLockMapper extends BaseMapper<DocEditLock> {
}
