package com.military.doc.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.system.entity.SysDict;
import com.military.doc.modules.system.mapper.SysDictMapper;
import com.military.doc.modules.system.service.SysDictService;
import org.springframework.stereotype.Service;

@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictService {
}
