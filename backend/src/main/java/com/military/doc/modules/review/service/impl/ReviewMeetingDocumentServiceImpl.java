package com.military.doc.modules.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.review.entity.ReviewMeetingDocument;
import com.military.doc.modules.review.mapper.ReviewMeetingDocumentMapper;
import com.military.doc.modules.review.service.ReviewMeetingDocumentService;
import org.springframework.stereotype.Service;

@Service
public class ReviewMeetingDocumentServiceImpl extends ServiceImpl<ReviewMeetingDocumentMapper, ReviewMeetingDocument> implements ReviewMeetingDocumentService {
}
