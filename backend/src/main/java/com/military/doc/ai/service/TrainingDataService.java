package com.military.doc.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.ai.entity.TrainingExample;

public interface TrainingDataService {
    TrainingExample collect(Long docFileId, Long projectId, Long catalogId, Long userId);
    TrainingExample approve(Long id);
    TrainingExample reject(Long id);
    Page<TrainingExample> list(String quality, int page, int size);
    String exportJsonl(String quality);
}
