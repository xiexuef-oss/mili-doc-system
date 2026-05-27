package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.document.entity.DocLedgerLog;
import com.military.doc.modules.document.mapper.DocLedgerLogMapper;
import com.military.doc.modules.document.service.DocLedgerLogService;
import org.springframework.stereotype.Service;

@Service
public class DocLedgerLogServiceImpl extends ServiceImpl<DocLedgerLogMapper, DocLedgerLog>
        implements DocLedgerLogService {
}
