package com.military.doc.ai.context;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SemanticMatch {
    private Long id;
    private String clauseNumber;
    private String clauseTitle;
    private String clauseContent;
    private String keywords;
    private String standardCode;
    private String standardName;
    private String category;
    private double similarity;
}
