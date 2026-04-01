package com.speedyjob.matcher.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 优化建议
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OptimizationSuggestion {
    /** 建议类别: KEYWORD / EXPERIENCE / SKILL / FORMAT / QUANTIFY */
    private String category;
    /** 优先级 1-5 (5最高) */
    private int priority;
    /** 建议标题 */
    private String title;
    /** 详细描述 */
    private String description;
    /** 原始内容 (如适用) */
    private String originalText;
    /** 建议改写内容 (如适用) */
    private String suggestedText;
}
