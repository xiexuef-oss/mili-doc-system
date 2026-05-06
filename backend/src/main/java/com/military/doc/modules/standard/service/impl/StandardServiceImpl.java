package com.military.doc.modules.standard.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.standard.entity.Standard;
import com.military.doc.modules.standard.mapper.StandardMapper;
import com.military.doc.modules.standard.service.StandardService;
import org.springframework.stereotype.Service;

@Service
public class StandardServiceImpl extends ServiceImpl<StandardMapper, Standard> implements StandardService {
}
