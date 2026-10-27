package cn.edu.campus.news.controller;

import cn.edu.campus.news.service.DatabaseProbe;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectController {
    private final DatabaseProbe databaseProbe;

    public ProjectController(DatabaseProbe databaseProbe) {
        this.databaseProbe = databaseProbe;
    }

    @GetMapping("/health")
    public Health health() {
        return new Health("UP", "campus-news-backend", "week1", "后端服务运行正常");
    }

    @GetMapping("/health/database")
    public ResponseEntity<DatabaseProbe.Result> database() {
        var result = databaseProbe.check();
        return ResponseEntity.status("DOWN".equals(result.status())
                ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK).body(result);
    }

    @GetMapping("/project")
    public ProjectInfo project() {
        return new ProjectInfo("校园新闻及通知发布系统设计及实现", "第一周 需求分析与环境准备",
                List.of("Spring Boot", "Vue", "MySQL"),
                List.of(new Role("访客", "浏览已发布的新闻和通知，按分类及关键词查询"),
                        new Role("发布人员", "登录后维护本人创建的内容，发布或撤回本人内容"),
                        new Role("管理员", "管理分类、账号及全部内容")),
                List.of("草稿 DRAFT", "已发布 PUBLISHED", "已撤回 WITHDRAWN"),
                List.of("需求范围、核心用例与数据清单", "前后端可启动工程及连接检查接口",
                        "MySQL 表结构草案与样例内容", "Git 忽略规则和手动建仓指南"),
                List.of("登录与权限验证", "新闻和通知增删改查", "发布撤回与前台可见性控制",
                        "分类、搜索、分页与完整业务测试"));
    }

    public record Health(String status, String service, String stage, String message) { }
    public record Role(String name, String description) { }
    public record ProjectInfo(String title, String phase, List<String> technologies,
                              List<Role> roles, List<String> contentStatuses,
                              List<String> weekOneDeliverables, List<String> plannedFeatures) { }
}
