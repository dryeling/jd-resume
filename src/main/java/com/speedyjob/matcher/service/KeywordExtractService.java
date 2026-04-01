package com.speedyjob.matcher.service;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 关键词提取服务
 * 使用 HanLP 中文分词 + 自定义技术词典 + 正则提取
 */
@Service
public class KeywordExtractService {

    /** 技术技能关键词词典 (覆盖主流技术栈) */
    private static final Set<String> TECH_KEYWORDS = new HashSet<>(Arrays.asList(
            // 编程语言
            "java", "python", "javascript", "typescript", "c++", "c#", "go", "golang", "rust",
            "php", "ruby", "swift", "kotlin", "scala", "r语言", "matlab", "shell", "bash",
            // 前端
            "react", "vue", "angular", "html", "css", "webpack", "vite", "nextjs", "nuxt",
            "tailwind", "bootstrap", "jquery", "sass", "less", "antd", "element-ui",
            // 后端框架
            "spring", "springboot", "spring boot", "spring cloud", "mybatis", "hibernate",
            "django", "flask", "fastapi", "express", "nestjs", "gin", "beego",
            // 数据库
            "mysql", "postgresql", "mongodb", "redis", "elasticsearch", "oracle",
            "sql server", "sqlite", "cassandra", "hbase", "clickhouse", "tidb",
            // 中间件
            "kafka", "rabbitmq", "rocketmq", "nginx", "tomcat", "zookeeper", "nacos",
            "apollo", "dubbo", "grpc", "netty",
            // 云与容器
            "docker", "kubernetes", "k8s", "aws", "阿里云", "腾讯云", "azure", "gcp",
            "jenkins", "ci/cd", "cicd", "terraform", "ansible",
            // 大数据
            "hadoop", "spark", "flink", "hive", "presto", "airflow", "数据仓库",
            "etl", "数据分析", "数据挖掘", "数据治理",
            // AI/ML
            "机器学习", "深度学习", "自然语言处理", "nlp", "计算机视觉", "cv",
            "tensorflow", "pytorch", "sklearn", "大模型", "llm", "gpt", "transformer",
            "rag", "向量数据库", "langchain", "prompt",
            // 通用能力
            "微服务", "分布式", "高并发", "高可用", "性能优化", "系统设计",
            "设计模式", "架构设计", "领域驱动", "ddd", "tdd", "敏捷开发",
            "scrum", "devops", "全栈", "restful", "api", "graphql",
            // 软技能 & 业务
            "项目管理", "团队管理", "需求分析", "产品设计", "用户研究",
            "数据驱动", "ab测试", "增长", "商业分析", "沟通能力", "领导力",
            // 行业
            "金融", "电商", "社交", "游戏", "教育", "医疗", "物流", "saas",
            "b2b", "b2c", "to b", "to c", "互联网", "人工智能"
    ));

    /** 教育相关关键词 */
    private static final Set<String> EDU_KEYWORDS = new HashSet<>(Arrays.asList(
            "本科", "硕士", "博士", "学士", "研究生", "mba", "985", "211", "双一流",
            "bachelor", "master", "phd", "大专", "专科", "统招"
    ));

    /** 经验年限模式 */
    private static final Pattern EXP_PATTERN = Pattern.compile(
            "(\\d+)\\s*[-~至到]?\\s*(\\d+)?\\s*年[以上]*[工作经验]*|" +
            "(\\d+)\\s*[+]?\\s*years?",
            Pattern.CASE_INSENSITIVE
    );

    /** 数量化指标模式 */
    private static final Pattern QUANTIFY_PATTERN = Pattern.compile(
            "(\\d+[%万亿+kKmM]|\\d+\\s*倍|\\d+\\s*个|百万|千万|提升\\s*\\d+|降低\\s*\\d+|节省\\s*\\d+)"
    );

    /**
     * 从文本中提取技术/技能关键词
     */
    public List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        String lowerText = text.toLowerCase();
        Set<String> found = new LinkedHashSet<>();

        // 1. 匹配预定义技术词典
        for (String keyword : TECH_KEYWORDS) {
            if (lowerText.contains(keyword.toLowerCase())) {
                found.add(keyword);
            }
        }

        // 2. 使用 HanLP 提取关键词补充
        try {
            List<String> hanlpKeywords = HanLP.extractKeyword(text, 30);
            for (String kw : hanlpKeywords) {
                if (kw.length() >= 2) {
                    found.add(kw);
                }
            }
        } catch (Exception e) {
            // HanLP 异常时降级, 不影响主流程
        }

        return new ArrayList<>(found);
    }

    /**
     * 提取教育要求
     */
    public List<String> extractEducationRequirements(String text) {
        if (text == null) return Collections.emptyList();
        String lowerText = text.toLowerCase();
        return EDU_KEYWORDS.stream()
                .filter(kw -> lowerText.contains(kw))
                .collect(Collectors.toList());
    }

    /**
     * 提取经验年限要求
     */
    public Optional<Integer> extractYearsRequired(String text) {
        if (text == null) return Optional.empty();
        Matcher matcher = EXP_PATTERN.matcher(text);
        if (matcher.find()) {
            String g1 = matcher.group(1);
            String g3 = matcher.group(3);
            if (g1 != null) return Optional.of(Integer.parseInt(g1));
            if (g3 != null) return Optional.of(Integer.parseInt(g3));
        }
        return Optional.empty();
    }

    /**
     * 检测文本中是否包含量化指标
     */
    public boolean hasQuantifiedResults(String text) {
        if (text == null) return false;
        return QUANTIFY_PATTERN.matcher(text).find();
    }

    /**
     * 提取所有量化指标
     */
    public List<String> extractQuantifiedMetrics(String text) {
        if (text == null) return Collections.emptyList();
        List<String> results = new ArrayList<>();
        Matcher matcher = QUANTIFY_PATTERN.matcher(text);
        while (matcher.find()) {
            results.add(matcher.group());
        }
        return results;
    }
}
