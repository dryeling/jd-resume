package com.speedyjob.matcher.service;

import com.speedyjob.matcher.model.MatchResult;
import com.speedyjob.matcher.model.OptimizationSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 优化建议生成服务
 * 根据匹配分析结果，生成具体可操作的简历优化建议
 */
@Service
public class SuggestionService {

    @Autowired
    private KeywordExtractService keywordService;

    /**
     * 根据匹配结果生成优化建议列表
     */
    public List<OptimizationSuggestion> generate(MatchResult result, String resumeText, String jdText) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        // 1. 缺失关键词建议
        addKeywordSuggestions(suggestions, result);

        // 2. 量化指标建议
        addQuantifySuggestions(suggestions, resumeText);

        // 3. 经验匹配建议
        addExperienceSuggestions(suggestions, result, resumeText, jdText);

        // 4. 教育匹配建议
        addEducationSuggestions(suggestions, result);

        // 5. 格式建议
        addFormatSuggestions(suggestions, resumeText);

        // 按优先级排序
        suggestions.sort((a, b) -> b.getPriority() - a.getPriority());
        return suggestions;
    }

    /**
     * 生成优化后的简历文本（自动补充关键词提示）
     */
    public String generateOptimizedResume(String resumeText, MatchResult result) {
        StringBuilder sb = new StringBuilder(resumeText);

        if (!result.getMissingKeywords().isEmpty()) {
            sb.append("\n\n--- 以下为 AI 建议补充的内容 ---\n");
            sb.append("【建议补充的技能关键词】\n");
            List<String> top = result.getMissingKeywords().stream().limit(5).toList();
            for (String kw : top) {
                sb.append("• ").append(kw)
                  .append(" - 建议在项目经验中体现相关实践\n");
            }
        }

        if (result.getDimensionScores() != null
                && result.getDimensionScores().getExperienceMatch() < 70) {
            sb.append("\n【经验描述优化建议】\n");
            sb.append("• 建议使用 STAR 法则描述项目经验\n");
            sb.append("• 添加具体的量化成果数据\n");
        }

        return sb.toString();
    }

    private void addKeywordSuggestions(List<OptimizationSuggestion> list, MatchResult result) {
        List<String> missing = result.getMissingKeywords();
        if (missing == null || missing.isEmpty()) return;

        // 高优先级缺失关键词
        List<MatchResult.SkillGap> highGaps = result.getSkillGaps().stream()
                .filter(g -> "HIGH".equals(g.getImportance()))
                .toList();

        if (!highGaps.isEmpty()) {
            String skills = highGaps.stream()
                    .map(MatchResult.SkillGap::getSkill)
                    .reduce((a, b) -> a + "、" + b).orElse("");
            list.add(OptimizationSuggestion.builder()
                    .category("KEYWORD")
                    .priority(5)
                    .title("补充核心技能关键词")
                    .description("JD 中的核心要求 [" + skills + "] 在简历中未体现。"
                            + "建议在技能清单或项目经验中明确提及这些技术。")
                    .suggestedText("在「技能」栏添加: " + skills)
                    .build());
        }

        if (missing.size() > highGaps.size()) {
            long others = missing.size() - highGaps.size();
            list.add(OptimizationSuggestion.builder()
                    .category("KEYWORD")
                    .priority(3)
                    .title("补充其他相关关键词")
                    .description("还有 " + others + " 个JD关键词未在简历中出现，"
                            + "建议适当补充以提高 ATS 系统通过率。")
                    .build());
        }
    }

    private void addQuantifySuggestions(List<OptimizationSuggestion> list, String resume) {
        boolean hasQuantified = keywordService.hasQuantifiedResults(resume);
        if (!hasQuantified) {
            list.add(OptimizationSuggestion.builder()
                    .category("QUANTIFY")
                    .priority(4)
                    .title("添加量化成果数据")
                    .description("简历中缺少量化指标（如百分比、数量、金额等）。"
                            + "数据化的成果描述能显著提升简历说服力。")
                    .originalText("例: 负责后端系统开发")
                    .suggestedText("例: 负责后端系统开发，QPS 从 500 提升至 3000，接口响应时间降低 60%")
                    .build());
        }
    }

    private void addExperienceSuggestions(List<OptimizationSuggestion> list,
                                          MatchResult result, String resume, String jd) {
        if (result.getDimensionScores() == null) return;
        int expScore = result.getDimensionScores().getExperienceMatch();
        if (expScore < 70) {
            Optional<Integer> jdYears = keywordService.extractYearsRequired(jd);
            String yearInfo = jdYears.map(y -> "JD要求 " + y + " 年经验，").orElse("");
            list.add(OptimizationSuggestion.builder()
                    .category("EXPERIENCE")
                    .priority(4)
                    .title("强化工作经验描述")
                    .description(yearInfo + "建议突出相关领域的工作年限，"
                            + "并用 STAR 法则（情境-任务-行动-结果）重写经验描述。")
                    .build());
        }
    }

    private void addEducationSuggestions(List<OptimizationSuggestion> list, MatchResult result) {
        if (result.getDimensionScores() == null) return;
        int eduScore = result.getDimensionScores().getEducationMatch();
        if (eduScore < 70) {
            list.add(OptimizationSuggestion.builder()
                    .category("EDUCATION")
                    .priority(2)
                    .title("优化教育背景展示")
                    .description("教育背景与JD要求存在差距，建议补充相关课程、认证或培训经历。")
                    .build());
        }
    }

    private void addFormatSuggestions(List<OptimizationSuggestion> list, String resume) {
        if (resume.length() < 200) {
            list.add(OptimizationSuggestion.builder()
                    .category("FORMAT")
                    .priority(3)
                    .title("简历内容过于简短")
                    .description("当前简历内容不足 200 字，建议丰富项目经验和技能描述，"
                            + "一般建议简历控制在 1-2 页 A4 篇幅。")
                    .build());
        }
        if (resume.length() > 3000) {
            list.add(OptimizationSuggestion.builder()
                    .category("FORMAT")
                    .priority(2)
                    .title("简历内容过长")
                    .description("简历超过 3000 字，建议精简非核心内容，"
                            + "突出与目标岗位最相关的经历。")
                    .build());
        }
    }
}
