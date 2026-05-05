package com.military.doc.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.military.doc.modules.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}