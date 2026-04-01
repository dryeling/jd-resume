package com.speedyjob.matcher.service;

import com.speedyjob.matcher.model.MatchResult;
import com.speedyjob.matcher.model.MatchResult.DimensionScores;
import com.speedyjob.matcher.model.MatchResult.SkillGap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 匹配分析核心服务
 * 负责将 JD 和简历进行多维度匹配评分
 */
@Service
public class MatchAnalysisService {

    @Autowired
    private KeywordExtractService keywordService;

    /**
     * 执行匹配分析，返回各维度评分及关键词匹配情况
     */
    public MatchResult analyze(String resumeText, String jdText) {
        // 1. 提取关键词
        List<String> jdKeywords = keywordService.extractKeywords(jdText);
        List<String> resumeKeywords = keywordService.extractKeywords(resumeText);

        // 2. 计算匹配与缺失
        String lowerResume = resumeText.toLowerCase();
        List<String> matched = jdKeywords.stream()
                .filter(kw -> lowerResume.contains(kw.toLowerCase()))
                .collect(Collectors.toList());
        List<String> missing = jdKeywords.stream()
                .filter(kw -> !lowerResume.contains(kw.toLowerCase()))
                .collect(Collectors.toList());

        // 3. 各维度评分
        int skillMatch = calcSkillMatch(jdKeywords, matched);
        int expMatch = calcExperienceMatch(resumeText, jdText);
        int eduMatch = calcEducationMatch(resumeText, jdText);
        int kwCoverage = jdKeywords.isEmpty() ? 0
                : (int) (matched.size() * 100.0 / jdKeywords.size());
        int industryRelevance = calcIndustryRelevance(resumeText, jdText);

        DimensionScores scores = DimensionScores.builder()
                .skillMatch(skillMatch)
                .experienceMatch(expMatch)
                .educationMatch(eduMatch)
                .keywordCoverage(kwCoverage)
                .industryRelevance(industryRelevance)
                .build();

        // 4. 综合分 (加权)
        int overall = (int) (skillMatch * 0.35 + expMatch * 0.2
                + eduMatch * 0.15 + kwCoverage * 0.2 + industryRelevance * 0.1);

        // 5. 技能差距
        List<SkillGap> gaps = buildSkillGaps(missing, jdText);

        // 6. 匹配等级
        String level = overall >= 85 ? "优秀匹配" :
                overall >= 70 ? "良好匹配" :
                overall >= 50 ? "一般匹配" : "匹配较弱";

        return MatchResult.builder()
                .overallScore(Math.min(overall, 100))
                .dimensionScores(scores)
                .jdKeywords(jdKeywords)
                .matchedKeywords(matched)
                .missingKeywords(missing)
                .skillGaps(gaps)
                .matchLevel(level)
                .build();
    }

    private int calcSkillMatch(List<String> jdKw, List<String> matched) {
        if (jdKw.isEmpty()) return 60;
        double ratio = matched.size() * 1.0 / jdKw.size();
        return Math.min((int) (ratio * 100), 100);
    }

    private int calcExperienceMatch(String resume, String jd) {
        Optional<Integer> jdYears = keywordService.extractYearsRequired(jd);
        Optional<Integer> resumeYears = keywordService.extractYearsRequired(resume);
        if (jdYears.isEmpty()) return 75; // JD未明确要求
        if (resumeYears.isEmpty()) return 50; // 简历未体现
        int req = jdYears.get();
        int have = resumeYears.get();
        if (have >= req) return 100;
        if (have >= req - 1) return 80;
        return Math.max(40, (int) (have * 100.0 / req));
    }

    private int calcEducationMatch(String resume, String jd) {
        List<String> jdEdu = keywordService.extractEducationRequirements(jd);
        List<String> resumeEdu = keywordService.extractEducationRequirements(resume);
        if (jdEdu.isEmpty()) return 75;
        if (resumeEdu.isEmpty()) return 50;

        // 简单逻辑: 学历层级映射
        Map<String, Integer> levels = Map.of(
                "大专", 1, "专科", 1, "本科", 2, "学士", 2,
                "硕士", 3, "研究生", 3, "博士", 4
        );
        int jdMax = jdEdu.stream().mapToInt(e -> levels.getOrDefault(e, 0)).max().orElse(0);
        int rMax = resumeEdu.stream().mapToInt(e -> levels.getOrDefault(e, 0)).max().orElse(0);
        if (rMax >= jdMax) return 100;
        if (rMax == jdMax - 1) return 70;
        return 45;
    }

    private int calcIndustryRelevance(String resume, String jd) {
        List<String> jdKw = keywordService.extractKeywords(jd);
        List<String> resumeKw = keywordService.extractKeywords(resume);
        Set<String> common = new HashSet<>(jdKw);
        common.retainAll(new HashSet<>(resumeKw));
        if (jdKw.isEmpty()) return 60;
        double ratio = common.size() * 1.0 / jdKw.size();
        return Math.min((int) (ratio * 120), 100); // 略微放大
    }

    private List<SkillGap> buildSkillGaps(List<String> missing, String jd) {
        String lowerJd = jd.toLowerCase();
        return missing.stream().limit(10).map(skill -> {
            // 通过在 JD 中出现的位置判断重要性 (越靠前越重要)
            int idx = lowerJd.indexOf(skill.toLowerCase());
            String importance;
            if (idx >= 0 && idx < lowerJd.length() / 3) {
                importance = "HIGH";
            } else if (idx >= 0 && idx < lowerJd.length() * 2 / 3) {
                importance = "MEDIUM";
            } else {
                importance = "LOW";
            }
            return SkillGap.builder()
                    .skill(skill)
                    .importance(importance)
                    .suggestion("建议在简历中补充 " + skill + " 相关经验或项目描述")
                    .build();
        }).collect(Collectors.toList());
    }
}
