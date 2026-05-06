package com.military.doc.modules.standard.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.mapper.StandardClauseMapper;
import com.military.doc.modules.standard.service.StandardClauseService;
import org.springframework.stereotype.Service;

@Service
public class StandardClauseServiceImpl extends ServiceImpl<StandardClauseMapper, StandardClause> implements StandardClauseService {
}
