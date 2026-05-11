package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_ledger_log")
public class DocLedgerLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docLedgerId;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private LocalDateTime operatedAt;
    private String remark;
}
