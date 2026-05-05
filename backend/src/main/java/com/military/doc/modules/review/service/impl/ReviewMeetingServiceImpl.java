package com.military.doc.modules.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.review.entity.ReviewMeeting;
import com.military.doc.modules.review.mapper.ReviewMeetingMapper;
import com.military.doc.modules.review.service.ReviewMeetingService;
import org.springframework.stereotype.Service;

@Service
public class ReviewMeetingServiceImpl extends ServiceImpl<ReviewMeetingMapper, ReviewMeeting> implements ReviewMeetingService {
}
