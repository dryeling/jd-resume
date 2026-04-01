package com.speedyjob.matcher.model;

import lombok.Data;

/**
 * 匹配分析请求
 */
@Data
public class MatchRequest {
    /** 简历文本 */
    private String resumeText;
    /** JD 文本 */
    private String jdText;
    /** 目标岗位名称 (可选) */
    private String targetPosition;
}
