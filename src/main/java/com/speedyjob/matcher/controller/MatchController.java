package com.speedyjob.matcher.controller;

import com.speedyjob.matcher.model.MatchRequest;
import com.speedyjob.matcher.model.MatchResult;
import com.speedyjob.matcher.model.OptimizationSuggestion;
import com.speedyjob.matcher.service.MatchAnalysisService;
import com.speedyjob.matcher.service.SuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/match")
@CrossOrigin(origins = "*")
public class MatchController {

    @Autowired
    private MatchAnalysisService analysisService;

    @Autowired
    private SuggestionService suggestionService;

    /**
     * 一站式匹配分析 + 优化建议
     */
    @PostMapping("/analyze")
    public ResponseEntity<MatchResult> analyze(@RequestBody MatchRequest request) {
        if (request.getResumeText() == null || request.getResumeText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getJdText() == null || request.getJdText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 1. 匹配分析
        MatchResult result = analysisService.analyze(
                request.getResumeText(), request.getJdText());

        // 2. 生成优化建议
        List<OptimizationSuggestion> suggestions = suggestionService.generate(
                result, request.getResumeText(), request.getJdText());
        result.setSuggestions(suggestions);

        // 3. 生成优化简历
        String optimized = suggestionService.generateOptimizedResume(
                request.getResumeText(), result);
        result.setOptimizedResume(optimized);

        return ResponseEntity.ok(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "JD-Resume Matcher v1.0"));
    }
}
