const API_BASE = '';  // 同域部署, 空字符串即可

// 示例数据
function fillSampleResume() {
    document.getElementById('resumeInput').value =
`张三 | Java 高级开发工程师
手机: 138xxxx8888 | 邮箱: zhangsan@email.com

【教育背景】
武汉大学 | 计算机科学与技术 | 本科 | 2015-2019

【技能清单】
- 编程语言: Java, Python, SQL
- 框架: Spring Boot, MyBatis
- 数据库: MySQL, Redis
- 工具: Git, Maven, Docker

【工作经历】
ABC科技有限公司 | 后端开发工程师 | 2019.07 - 至今 (5年)
- 负责公司核心电商平台后端系统开发与维护
- 参与订单系统重构, 使用 Spring Boot 微服务架构
- 实现了基于 Redis 的缓存方案, 减少数据库查询压力
- 使用 MySQL 进行数据存储, 编写复杂 SQL 查询

【项目经历】
电商订单系统2.0
- 基于 Spring Boot + MyBatis 搭建后端服务
- 负责订单模块和支付模块的开发
- 使用 Docker 容器化部署`;
}

function fillSampleJD() {
    document.getElementById('jdInput').value =
`高级 Java 开发工程师

【职位要求】
1. 本科及以上学历, 计算机相关专业, 3年以上 Java 开发经验
2. 精通 Java 编程, 熟悉 Spring Boot, Spring Cloud 微服务架构
3. 熟悉 MySQL, Redis, Elasticsearch 等存储组件
4. 熟悉 Kafka 或 RabbitMQ 等消息中间件
5. 了解分布式系统设计, 有高并发系统开发经验优先
6. 熟悉 Docker, Kubernetes 容器化技术
7. 良好的沟通能力和团队协作精神

【职责描述】
1. 负责公司核心业务系统的架构设计和开发
2. 参与微服务架构的设计与落地
3. 负责系统性能优化, 保障系统高可用
4. 编写技术文档, 参与 Code Review

【福利待遇】
- 薪资: 25K-40K
- 五险一金, 年终奖, 弹性工作`;
}

async function doAnalyze() {
    const resumeText = document.getElementById('resumeInput').value.trim();
    const jdText = document.getElementById('jdInput').value.trim();

    if (!resumeText) { alert('请输入简历内容'); return; }
    if (!jdText) { alert('请输入目标职位 JD'); return; }

    const btn = document.getElementById('analyzeBtn');
    btn.disabled = true;
    document.getElementById('loadingArea').style.display = 'block';
    document.getElementById('resultArea').style.display = 'none';

    try {
        const response = await fetch(API_BASE + '/api/match/analyze', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ resumeText, jdText })
        });

        if (!response.ok) throw new Error('请求失败: ' + response.status);
        const data = await response.json();
        renderResult(data);
    } catch (e) {
        alert('分析失败: ' + e.message);
    } finally {
        btn.disabled = false;
        document.getElementById('loadingArea').style.display = 'none';
    }
}

function renderResult(data) {
    document.getElementById('resultArea').style.display = 'block';

    // 1. 分数环动画
    const score = data.overallScore || 0;
    const circle = document.getElementById('scoreCircle');
    const circumference = 2 * Math.PI * 70; // ~440
    const offset = circumference - (score / 100) * circumference;
    circle.style.stroke = getScoreColor(score);
    setTimeout(() => { circle.style.strokeDashoffset = offset; }, 100);
    animateNumber('scoreNumber', 0, score, 1000);

    // 匹配等级
    const levelEl = document.getElementById('matchLevel');
    levelEl.textContent = data.matchLevel || '--';
    levelEl.className = 'match-level ' + getLevelClass(score);

    // 2. 维度条
    renderDimensions(data.dimensionScores);

    // 3. 关键词标签
    renderKeywords(data.matchedKeywords, data.missingKeywords);

    // 4. 优化建议
    renderSuggestions(data.suggestions);

    // 5. 优化简历
    document.getElementById('optimizedResume').textContent = data.optimizedResume || '';

    // 滚动到结果
    document.getElementById('resultArea').scrollIntoView({ behavior: 'smooth' });
}

function renderDimensions(scores) {
    if (!scores) return;
    const dims = [
        { label: '技能匹配度', value: scores.skillMatch, color: '#667eea' },
        { label: '经验匹配度', value: scores.experienceMatch, color: '#764ba2' },
        { label: '教育匹配度', value: scores.educationMatch, color: '#43a047' },
        { label: '关键词覆盖率', value: scores.keywordCoverage, color: '#f4511e' },
        { label: '行业相关度', value: scores.industryRelevance, color: '#00acc1' }
    ];
    const container = document.getElementById('dimensionBars');
    container.innerHTML = dims.map(d => `
        <div class="dim-item">
            <label>${d.label} <span class="dim-score">${d.value}分</span></label>
            <div class="dim-bar-bg">
                <div class="dim-bar-fg" style="width:${d.value}%;background:${d.color}"></div>
            </div>
        </div>
    `).join('');
}

function renderKeywords(matched, missing) {
    const container = document.getElementById('keywordTags');
    let html = '';
    if (matched) {
        matched.forEach(kw => { html += `<span class="kw-tag kw-matched">✅ ${kw}</span>`; });
    }
    if (missing) {
        missing.forEach(kw => { html += `<span class="kw-tag kw-missing">❌ ${kw}</span>`; });
    }
    container.innerHTML = html;
}

function renderSuggestions(suggestions) {
    const container = document.getElementById('suggestionList');
    if (!suggestions || suggestions.length === 0) {
        container.innerHTML = '<p style="color:#999">暂无优化建议, 您的简历匹配度很高!</p>';
        return;
    }
    container.innerHTML = suggestions.map(s => {
        let exampleHtml = '';
        if (s.originalText || s.suggestedText) {
            exampleHtml = `<div class="sug-example">`;
            if (s.originalText) exampleHtml += `<div class="before">原: ${s.originalText}</div>`;
            if (s.suggestedText) exampleHtml += `<div class="after">改: ${s.suggestedText}</div>`;
            exampleHtml += `</div>`;
        }
        return `
        <div class="suggestion-item">
            <div class="sug-header">
                <div class="sug-priority priority-${s.priority}">${s.priority}</div>
                <span class="sug-title">${s.title}</span>
                <span class="sug-cat">${getCategoryLabel(s.category)}</span>
            </div>
            <div class="sug-desc">${s.description}</div>
            ${exampleHtml}
        </div>`;
    }).join('');
}

function getCategoryLabel(cat) {
    const map = { KEYWORD: '关键词', EXPERIENCE: '经验', SKILL: '技能',
                  FORMAT: '格式', QUANTIFY: '量化', EDUCATION: '教育' };
    return map[cat] || cat;
}

function getScoreColor(score) {
    if (score >= 85) return '#43a047';
    if (score >= 70) return '#1e88e5';
    if (score >= 50) return '#f4511e';
    return '#c62828';
}

function getLevelClass(score) {
    if (score >= 85) return 'level-excellent';
    if (score >= 70) return 'level-good';
    if (score >= 50) return 'level-fair';
    return 'level-weak';
}

function animateNumber(elementId, start, end, duration) {
    const el = document.getElementById(elementId);
    const range = end - start;
    const startTime = performance.now();
    function step(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        el.textContent = Math.round(start + range * progress);
        if (progress < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
}
