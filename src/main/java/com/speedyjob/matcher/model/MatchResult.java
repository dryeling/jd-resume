package com.speedyjob.matcher.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 匹配分析结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchResult {
    /** 总体匹配分数 0-100 */
    private int overallScore;

    /** 各维度得分 */
    private DimensionScores dimensionScores;

    /** JD 中提取的关键词 */
    private List<String> jdKeywords;

    /** 简历中匹配到的关键词 */
    private List<String> matchedKeywords;

    /** 简历中缺失的关键词 */
    private List<String> missingKeywords;

    /** 技能差距分析 */
    private List<SkillGap> skillGaps;

    /** 优化建议 */
    private List<OptimizationSuggestion> suggestions;

    /** 优化后的简历文本 */
    private String optimizedResume;

    /** 匹配等级 */
    private String matchLevel;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DimensionScores {
        /** 技能匹配度 */
        private int skillMatch;
        /** 经验匹配度 */
        private int experienceMatch;
        /** 教育匹配度 */
        private int educationMatch;
        /** 关键词覆盖率 */
        private int keywordCoverage;
        /** 行业相关度 */
        private int industryRelevance;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillGap {
        /** 技能名称 */
        private String skill;
        /** 重要程度 HIGH/MEDIUM/LOW */
        private String importance;
        /** 建议补充描述 */
        private String suggestion;
    }
}
